import tellcore.constants as const
from GetLightDevices import get_devices_light
lights_status = []

devices_light = get_devices_light()
if len(devices_light) == 0:
    raise ValueError("No light actuator connected")

for light in devices_light:
    print light.last_sent_command(const.TELLSTICK_TURNON | const.TELLSTICK_TURNOFF) == const.TELLSTICK_TURNON