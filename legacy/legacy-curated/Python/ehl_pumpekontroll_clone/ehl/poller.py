"""Polling-loop som etterligner `state_timer_Timer` i pumpekontroll.frm.

VB6 gjør på hvert timer-tikk:
1) send STATE (0x4B)
2) send TANK (0xC5)
3) hvis DISP_openfordelivery OR tank_vol >= tank_vol_last: send VOLUME (0x45)
4) hver 10. gang: send LINETEST (0x6A)

Den slår også av cmdstart.FontBold etter hvert tick, men det er UI.

Denne implementasjonen følger samme senderekkefølge og intervaller.
"""

from __future__ import annotations

import threading
import time
from dataclasses import dataclass
from typing import Optional

from .model import PumpekontrollState
from .serial_client import EhlSerialClient


@dataclass
class PollConfig:
    period_s: float = 1.0
    linetest_every: int = 10  # disptest_interval==10 or 0


class PumpekontrollPoller:
    def __init__(self, client: EhlSerialClient, state: PumpekontrollState, cfg: Optional[PollConfig] = None) -> None:
        self.client = client
        self.state = state
        self.cfg = cfg or PollConfig()

        self._stop = threading.Event()
        self._thread: Optional[threading.Thread] = None
        self._tick = 0

    def start(self) -> None:
        self._stop.clear()
        self._thread = threading.Thread(target=self._loop, name="EHL-Poller", daemon=True)
        self._thread.start()

    def stop(self) -> None:
        self._stop.set()
        if self._thread is not None:
            self._thread.join(timeout=1.0)

    def _loop(self) -> None:
        # disptest_interval starter med 0 i VB6
        disptest_interval = 0
        while not self._stop.is_set():
            t0 = time.time()

            # 1) STATE
            self.client.poll_state()

            # 2) TANK
            self.client.poll_tank()

            # 3) VOLUME betinget
            if self.state.DISP_openfordelivery or (self.state.tank_vol >= self.state.tank_vol_last):
                self.client.poll_volume()

            # 4) LINETEST hver 10. gang (eller første gang)
            if disptest_interval == self.cfg.linetest_every or disptest_interval == 0:
                self.client.poll_linetest()
                disptest_interval = 1

            disptest_interval += 1
            self._tick += 1

            # sov til neste tick
            dt = time.time() - t0
            to_sleep = max(0.0, self.cfg.period_s - dt)
            time.sleep(to_sleep)
