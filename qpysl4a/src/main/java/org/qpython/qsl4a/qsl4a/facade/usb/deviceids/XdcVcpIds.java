package org.qpython.qsl4a.qsl4a.facade.usb.deviceids;


import static org.qpython.qsl4a.qsl4a.facade.usb.deviceids.Helpers.createTable;
import static org.qpython.qsl4a.qsl4a.facade.usb.deviceids.Helpers.createDevice;

public class XdcVcpIds
{
    /*
     * Werner Wolfrum (w.wolfrum@wolfrum-elektronik.de)
     */

    /* Different products and vendors of XdcVcp family
     */
    private static final long[] xdcvcpDevices = createTable(
            createDevice(0x264D, 0x0232), // VCP (Virtual Com Port)
            createDevice(0x264D, 0x0120),  // USI (Universal Sensor Interface)
            createDevice(0x0483, 0x5740) //CC3D (STM)
    );

    public static boolean isDeviceSupported(int vendorId, int productId)
    {
        return Helpers.exists(xdcvcpDevices, vendorId, productId);
    }
}
