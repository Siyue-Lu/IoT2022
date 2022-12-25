import tellcore.telldus as td

core = td.TelldusCore()
devices = core.devices()

def get_device_heater():
    for device in devices:
        # find actuators for heater
        if device.name == "Heating":
            return device