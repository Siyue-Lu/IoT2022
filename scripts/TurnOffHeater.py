from GetHeaterDevice import get_device_heater

heater = get_device_heater()
if heater is None:
    raise ValueError("No heater actuator connected")

heater.turn_off()