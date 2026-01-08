"""Tilstand/variabler som speiler logikken i `pumpekontroll.frm`.

Målet her er ikke å kopiere UI/DB/Bank-funksjoner, men å kopiere:
- hvordan VB6 oppdaterer tilstand når den mottar STATE/VOLUME/PRICE/LINETEST/TANK
- og hvordan den bestemmer når en tanking anses ferdig (tank_vol stabil + trans_unaccounted)

Alt her er direkte utledet fra koden i MSComm1_OnComm (Select Case x(3)).
"""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
from typing import Optional

from .protocol import (
    decode_price_from_data,
    decode_volume_from_data,
    state_bits_from_byte,
    tank_bits_from_byte,
)


CMD_OK = 30
CMD_ERROR = 37
CMD_VOLUME = 69  # 0x45
CMD_STATE = 75   # 0x4B
CMD_PRICE = 92   # 0x5C
CMD_LINETEST = 106  # 0x6A
CMD_SUM = 133
CMD_TANK = 197  # 0xC5


@dataclass
class PumpekontrollState:
    """Runtime state som VB6 holder i form-variabler."""

    # "Station mode" (VB6 toggler via Dagstilling/Nattstilling/Stengt)
    station_closed: bool = True

    # Dispenser state bits (fra STATE)
    DISP_startbuttonpressed: bool = False
    DISP_openfordelivery: bool = False
    disp_automode: bool = False

    # TANK status bits
    trans_unaccounted: bool = False
    trans_finished_powerfault: bool = False

    # Line test init
    disp_init: bool = False

    # Pris/volum/sum
    tank_vol: float = 0.0
    tank_vol_last: float = 0.0
    tank_unitprice: float = 0.0
    tank_sum: float = 0.0

    # "new_tank" logikk
    new_tank: bool = False
    tank_end: bool = True
    tank_start_time: Optional[datetime] = None

    # UI-felter i VB6 (vi holder dem som tekst i CLI)
    dispris_text: str = ""

    # sist mottatt rå state bytes
    last_state_byte: Optional[int] = None
    last_tank_status_byte: Optional[int] = None

    # historikk / hendelser
    events: list[str] = field(default_factory=list)

    def log(self, msg: str) -> None:
        ts = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        self.events.append(f"{ts} {msg}")

    # --------- Handlere som speiler MSComm1_OnComm ---------

    def on_state(self, state_byte: int) -> None:
        self.last_state_byte = state_byte & 0xFF
        bits = state_bits_from_byte(state_byte)

        self.DISP_startbuttonpressed = bits["startbuttonpressed"]
        self.DISP_openfordelivery = bits["openfordelivery"]
        self.disp_automode = bits["automode"]

        # VB6:
        # If DISP_startbuttonpressed Then
        #    If new_tank = False Then new_tank=True; tank_end=False; SaveSetting start
        #    If DISP_openfordelivery Then reset vol/price/sum
        # Else
        #    new_tank = False
        # End If
        if self.DISP_startbuttonpressed:
            if not self.new_tank:
                self.new_tank = True
                self.tank_end = False
                self.tank_start_time = datetime.now()
                self.log("Ny tanking detektert (startknapp).")

            if self.DISP_openfordelivery:
                self.tank_vol = 0.0
                self.tank_vol_last = 0.0
                self.tank_unitprice = 0.0
                self.tank_sum = 0.0
                self.log("Dispenser open for delivery – nullstiller tankverdier.")
        else:
            if self.new_tank:
                self.log("Startknapp sluppet – new_tank=False.")
            self.new_tank = False

        if self.DISP_startbuttonpressed and (not self.DISP_openfordelivery) and (not self.station_closed):
            if self.disp_automode:
                # VB6 spiller lyd og setter cmdstart.FontBold = True
                self.log("AUTOMODE: startknapp trykket, klar for autorisering/start.")

    def on_tank_status(self, status_byte: int) -> None:
        self.last_tank_status_byte = status_byte & 0xFF
        bits = tank_bits_from_byte(status_byte)
        self.trans_finished_powerfault = bits["trans_finished_powerfault"]
        self.trans_unaccounted = bits["trans_unaccounted"]

    def on_price(self, data: bytes) -> None:
        price = decode_price_from_data(data)
        self.tank_unitprice = price
        # VB6 viser tekst: "pp.pp"
        self.dispris_text = f"{price:.2f}"

    def on_volume(self, data: bytes) -> bool:
        """Returnerer True hvis VB6 hadde sendt ZER/Reset i dette punktet."""

        vol = decode_volume_from_data(data)
        self.tank_vol = vol

        # VB6: If tank_vol_last = tank_vol And trans_unaccounted=True Then
        #        ... skriv DB/kvittering ... send reset
        if (self.tank_vol_last == self.tank_vol) and self.trans_unaccounted:
            # VB6 setter status=4, skriver kvittering osv.
            self.log(
                f"Tanking ferdig (vol stabil={self.tank_vol:.2f} L, trans_unaccounted=True)."
            )
            # Etter DB/print setter de tank_vol=0
            # Vi lar verdien stå, men signaliserer at det er på tide å sende reset.
            return True

        # Ellers: oppdater last, regn sum
        self.tank_vol_last = self.tank_vol
        self.tank_sum = self.tank_vol * self.tank_unitprice
        return False

    def on_linetest(self, data: bytes) -> None:
        # VB6: If x(4)=85 And x(5)=170 Then disp_init=True Else False
        if len(data) >= 2 and data[0] == 0x55 and data[1] == 0xAA:
            if not self.disp_init:
                self.log("LINETEST OK (55 AA) – disp_init=True")
            self.disp_init = True
        else:
            self.disp_init = False

