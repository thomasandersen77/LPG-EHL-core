import os
import sys
import unittest

# Gjør testen kjørbar uten installasjon: legg prosjektroten på sys.path
PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
if PROJECT_ROOT not in sys.path:
    sys.path.insert(0, PROJECT_ROOT)

from ehl.protocol import encode_frame, decode_frame, STX_CONTROLLER, STX_DISPENSER


class TestProtocol(unittest.TestCase):
    def test_state_request_addr1(self):
        # I pumpekontroll.frm bygges STATE-request slik:
        # 10 06 <addr> 4B chk 36
        raw = encode_frame(addr=1, cmd=0x4B, data=b"", from_controller=True)
        self.assertEqual(raw[0], STX_CONTROLLER)
        self.assertEqual(raw[1], 6)
        self.assertEqual(raw[-1], 0x36)

        # checksum = 10 xor 06 xor 01 xor 4B = 5C
        self.assertEqual(raw, bytes([0x10, 0x06, 0x01, 0x4B, 0x5C, 0x36]))

    def test_decode_volume_response(self):
        # Bygg en fake VOLUME-response fra dispenser:
        # 20 0B 01 45 d0..d4 chk 36
        # 45.50 L -> "04550" -> data LSB-first: '0','5','5','4','0'
        data = b"05540"  # d0..d4
        frame = bytes([0x20, 0x0B, 0x01, 0x45]) + data
        chk = 0
        for b in frame:
            chk ^= b
        raw = frame + bytes([chk, 0x36])

        decoded = decode_frame(raw, require_stx=STX_DISPENSER)
        self.assertEqual(decoded.cmd, 0x45)
        self.assertEqual(decoded.data, data)


if __name__ == "__main__":
    unittest.main()
