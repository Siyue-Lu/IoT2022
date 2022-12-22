import tellcore.telldus as td

core = td.TelldusCore()
sensors = core.sensors()
sensor_temp = None

for sensor in sensors:
    # find temperature sensor
    if sensor.has_temperature(): sensor_temp = sensor

if sensor_temp is None:
    raise ValueError("No temperature sensor connected")

print sensor_temp.temperature().value