#!/usr/bin/env python3
Pumpekontroll

Dette er et program som implementerer:
- EHL framing + checksum (STX=0x10/0x20, LEN, XOR, ETX=0x36)
- samme polling-loop som VB6 `state_timer_Timer`
- samme tolkning av STATE/VOLUME/PRICE/LINETEST/TANK som VB6 `MSComm1_OnComm`
- samme startsekvens som VB6 `cmdstart_Click` (PRESTART C3 30 + UNBLOCK 77)
- samme stop/"block" som VB6 `cmddisp_stop_Click` (BLOCK 69)
- samme reset som VB6 sender når tanking anses ferdig (ZER/RESET 81)

Viktig:
- Programmet dekker *kun* det som er eksplisitt i pumpekontroll.frm.
- Det inkluderer ikke DB, bank (baxi.dll) eller printerlogikk.

Kjøring:
  pip install -r requirements.txt
  python pumpekontroll_clone.py --port COM5 --dispenser-nr 1 --unit-price 16.04

Interaktivt:
  start  -> PRESTART + UNBLOCK
  stop   -> BLOCK
  reset  -> ZER
  day|night|closed -> setter station-mode (påvirker automode-logg)
  status -> print status
  quit   -> avslutt
"""

from __future__ import annotations

import argparse
import sys
import threading
import time

from ehl.model import (
    CMD_ERROR,
    CMD_LINETEST,
    CMD_OK,
    CMD_PRICE,
    CMD_STATE,
    CMD_TANK,
    CMD_VOLUME,
    PumpekontrollState,
)
from ehl.poller import PumpekontrollPoller
from ehl.serial_client import EhlSerialClient, SerialConfig


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser()
    p.add_argument("--port", required=True, help="Seriell port (COM5, /dev/ttyUSB0, etc.)")
    p.add_argument(
        "--dispenser-nr",
        type=int,
        default=1,
        help="Historisk bruker VB6 ADDR = dispensernr + 32. Default 1 gir addr=33.",
    )
    p.add_argument(
        "--addr",
        type=int,
        default=None,
        help="Overstyr wire-adresse direkte (0-255). Hvis satt, ignorerer --dispenser-nr.",
    )
    p.add_argument(
        "--baudrate",
        type=int,
        default=9600,
        help="Baudrate. (VB6-settings står ikke eksplisitt i pumpekontroll.frm.)",
    )
    p.add_argument(
        "--unit-price",
        type=float,
        default=0.0,
        help="Pris pr liter (kr) brukt til å beregne sum, siden pumpekontroll tar den fra DB.",
    )
    p.add_argument(
        "--poll-period",
        type=float,
        default=1.0,
        help="Sekunder mellom poll-ticks (VB6 timer).",
    )
    return p.parse_args()


def main() -> int:
    args = parse_args()

    addr = args.addr if args.addr is not None else (args.dispenser_nr + 32)
    if not (0 <= addr <= 255):
        print(f"Ugyldig addr: {addr}", file=sys.stderr)
        return 2

    state = PumpekontrollState()
    state.tank_unitprice = float(args.unit_price or 0.0)

    # Callback fra serial reader
    client_holder = {"client": None}  # for å kunne sende reset fra callback

    def on_frame(frame):
        # Speiler Select Case x(3) i VB6.
        cmd = frame.cmd
        data = frame.data

        if cmd == CMD_OK:
            # (OK) Command acknowledgement
            return
        if cmd == CMD_ERROR:
            state.log("ERROR-ramme mottatt (CMD=37).")
            return

        if cmd == CMD_VOLUME:
            should_reset = state.on_volume(data)
            if should_reset:
                # VB6 sender ZER (0x81) med en gang
                try:
                    client_holder["client"].reset_zer()
                    state.log("Sendt RESET/ZER (0x81) etter ferdig tanking.")
                except Exception as e:
                    state.log(f"Klarte ikke sende RESET/ZER: {e}")
            return

        if cmd == CMD_STATE:
            if len(data) >= 1:
                state.on_state(data[0])
            return

        if cmd == CMD_PRICE:
            # Pumpekontroll kan motta PRICE, men i denne fila ser vi ikke poll av PRICE.
            try:
                state.on_price(data)
                state.log(f"Oppdatert pris fra PRICE-ramme: {state.dispris_text} kr/L")
            except Exception as e:
                state.log(f"Ugyldig PRICE-ramme: {e}")
            return

        if cmd == CMD_LINETEST:
            state.on_linetest(data)
            return

        if cmd == CMD_TANK:
            if len(data) >= 1:
                state.on_tank_status(data[0])
            return

        # Ukjent kommando
        state.log(f"Ukjent CMD=0x{cmd:02X} ({cmd}) data={data.hex(' ')}")

    cfg = SerialConfig(port=args.port, baudrate=args.baudrate)
    client = EhlSerialClient(cfg=cfg, addr=addr, on_frame=on_frame)
    client_holder["client"] = client

    print(f"Åpner port {args.port} (baud={args.baudrate}) addr={addr} ...")
    client.open()

    # Init/linetest tilsvarende Form_Load sin check_disp_com-loop:
    print("Sjekker kommunikasjon (LINETEST) ...")
    for i in range(1, 21):
        client.poll_linetest()
        time.sleep(0.05)
        if state.disp_init:
            break
    if not state.disp_init:
        print("Fikk ingen kommunikasjon med dispenser (disp_init=False etter 20 forsøk).")
        client.close()
        return 1

    print("OK: disp_init=True")

    # Start polling-loop
    poller = PumpekontrollPoller(client=client, state=state)
    poller.cfg.period_s = float(args.poll_period)
    poller.start()

    # Status-printer i bakgrunnen
    stop_ui = threading.Event()

    def status_loop():
        while not stop_ui.is_set():
            print_status(state)
            time.sleep(1.0)

    t = threading.Thread(target=status_loop, name="Status", daemon=True)
    t.start()

    print("\nKlar. Skriv kommando: start | stop | reset | day | night | closed | status | quit")

    try:
        while True:
            line = sys.stdin.readline()
            if not line:
                break
            cmd = line.strip().lower()
            if cmd in ("quit", "exit", "q"):
                break
            if cmd == "status":
                print_status(state, force=True)
                continue
            if cmd == "day":
                state.station_closed = False
                state.log("Stasjon satt til DAGSTILLING (station_closed=False).")
                continue
            if cmd == "night":
                state.station_closed = False
                state.log("Stasjon satt til NATTSTILLING (station_closed=False).")
                continue
            if cmd == "closed":
                state.station_closed = True
                state.log("Stasjon satt til STENGT (station_closed=True).")
                continue
            if cmd == "start":
                # VB6: PRESTART (C3 30) hvis beløp er skrevet inn + UNBLOCK
                client.prestart()
                client.unblock()
                state.log("Sendt PRESTART (C3 30) + UNBLOCK (77).")
                continue
            if cmd == "stop":
                client.block()
                state.log("Sendt BLOCK (69).")
                continue
            if cmd == "reset":
                client.reset_zer()
                state.log("Sendt RESET/ZER (81).")
                continue

            print("Ukjent kommando.")

    except KeyboardInterrupt:
        pass
    finally:
        stop_ui.set()
        poller.stop()
        client.close()

    print("Avsluttet.")
    return 0


def print_status(state: PumpekontrollState, force: bool = False) -> None:
    # Hold utskrift kort og nyttig.
    s = (
        f"init={int(state.disp_init)} "
        f"start={int(state.DISP_startbuttonpressed)} "
        f"open={int(state.DISP_openfordelivery)} "
        f"auto={int(state.disp_automode)} "
        f"unacc={int(state.trans_unaccounted)} "
        f"pf={int(state.trans_finished_powerfault)} "
        f"vol={state.tank_vol:6.2f}L "
        f"pris={state.tank_unitprice:5.2f} "
        f"sum={state.tank_sum:7.2f}"
    )
    print(s)

    # Print siste event hvis den endret seg.
    if state.events:
        last = state.events[-1]
        # Vi printer alltid hvis force; ellers bare når det er nytt på siste sekund.
        if force or (time.time() % 1.0 < 0.01):
            print("  ", last)


if __name__ == "__main__":
    raise SystemExit(main())
