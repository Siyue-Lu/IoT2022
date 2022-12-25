import tellcore.constants as const
from GetHeaterDevice import get_device_heater

heater = get_device_heater()
if heater is None:
    raise ValueError("No heater actuator connected")

print heater.last_sent_command(const.TELLSTICK_TURNON | const.TELLSTICK_TURNOFF) == const.TELLSTICK_TURNON