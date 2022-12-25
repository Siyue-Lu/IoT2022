from GetLightDevices import get_devices_light

devices_light = get_devices_light()
if len(devices_light) == 0:
    raise ValueError("No light actuator connected")
    
for light in devices_light:
    light.turn_off()