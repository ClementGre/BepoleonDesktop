package fr.clementgre.bepoleondesktop;

import org.hid4java.HidDevice;
import org.hid4java.HidServices;
import org.hid4java.HidServicesListener;
import org.hid4java.HidServicesSpecification;
import org.hid4java.event.HidServicesEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class HidManager implements HidServicesListener{

    public static final Logger logger = LoggerFactory.getLogger(HidManager.class);

    private final HidServices hidServices;
    private HidDevice device = null;
    private final HidDataListener listener;

    public interface HidDataListener {
        void onHidDataReceived(byte[] data);
    }

    public HidManager(HidDataListener listener) {
        this.listener = listener;

        HidServicesSpecification hidServicesSpecification = new HidServicesSpecification();
        hidServicesSpecification.setAutoDataRead(true);
        hidServicesSpecification.setAutoStart(false);
        hidServicesSpecification.setDataReadInterval(100);
        hidServices = org.hid4java.HidManager.getHidServices(hidServicesSpecification);
        hidServices.addHidServicesListener(this);
        hidServices.start();
    }

    public void findDevice() {
        for (HidDevice hidDevice : hidServices.getAttachedHidDevices()) {
            if (hidDevice.getUsage() == 0x61 && hidDevice.getUsagePage() == 0xffffff60) {
                logger.info("Device found: " + hidDevice.getProduct() + " - " + hidDevice.getManufacturer() + " - " + hidDevice.getSerialNumber());
                device = hidDevice;
                device.open();
                break;
            }
        }
    }
    public boolean isDeviceReady() {
        return device != null && !device.isClosed() && hidServices.getAttachedHidDevices().contains(device);
    }

    public void unregisterDevice(){
        if(device != null){
            logger.info("Unregistering device: " + device.getProduct() + " - " + device.getManufacturer() + " - " + device.getSerialNumber());
            device.close();
            device = null;
        }
    }

    @Override
    public void hidDeviceAttached(HidServicesEvent event) {
        if(event.getHidDevice().getUsage() == 0x61 && event.getHidDevice().getUsagePage() == 0xffffff60){
            logger.info("Device attached: " + event.getHidDevice().getProduct() + " - " + event.getHidDevice().getManufacturer() + " - " + event.getHidDevice().getSerialNumber());
            device = event.getHidDevice();
            device.open();
        }
    }
    @Override
    public void hidDeviceDetached(HidServicesEvent event) {
        if(event.getHidDevice() == device){
            logger.info("Device detached: " + event.getHidDevice().getProduct() + " - " + event.getHidDevice().getManufacturer() + " - " + event.getHidDevice().getSerialNumber());
            device.close();
            device = null;
        }
    }

    @Override
    public void hidFailure(HidServicesEvent event) {
        if (event.getHidDevice() != device) return;
        logger.error("HID Failure: " + event);
    }

    @Override
    public void hidDataReceived(HidServicesEvent event) {
        if (event.getHidDevice() != device) return;
        logger.debug("Received data: " + Arrays.toString(event.getDataReceived()));
        // Add code to the main queue to avoid concurrency issues
        Main.queue.add(() -> listener.onHidDataReceived(event.getDataReceived()));
    }

    public void sendData(byte[] data) {
        logger.debug("Sending data: " + Arrays.toString(data));
        device.write(data, 32, (byte) 0x00);
    }

}
