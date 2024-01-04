package fr.clementgre.bepoleondesktop;

import org.hid4java.HidDevice;
import org.hid4java.HidServices;
import org.hid4java.HidServicesSpecification;

import java.util.Arrays;

public class HidManager {

    private final HidServices hidServices;
    private HidDevice device = null;

    public HidManager() {
        HidServicesSpecification hidServicesSpecification = new HidServicesSpecification();
        hidServicesSpecification.setAutoStart(false);
        hidServices = org.hid4java.HidManager.getHidServices(hidServicesSpecification);
        hidServices.start();
        //hidServices.addHidServicesListener(new Main());
        findDevice();
    }

    public void findDevice() {
        for (HidDevice hidDevice : hidServices.getAttachedHidDevices()) {
            System.out.println(hidDevice);
            if (hidDevice.getUsage() == 0x61 && hidDevice.getUsagePage() == 0xffffff60) {
                device = hidDevice;
                device.open();
                System.out.println("Device found");
                break;
            }
        }
    }
    public boolean isDeviceReady() {
        return device != null && !device.isClosed() && hidServices.getAttachedHidDevices().contains(device);
    }

    public void sendData(byte[] data) {
        Main.logger.debug("Sending data: " + Arrays.toString(data));
        device.write(data, 32, (byte) 0x00);
    }

}
