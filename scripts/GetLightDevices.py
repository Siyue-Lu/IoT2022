import tellcore.telldus as td

core = td.TelldusCore()
devices = core.devices()
devices_light = []

def get_devices_light():
    for device in devices:
        # find actuators for light
        if device.name == "Lighting": devices_light.append(device)
    return devices_light