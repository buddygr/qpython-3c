package org.qpython.qsl4a.qsl4a.facade.usb.deviceids;

import static org.qpython.qsl4a.qsl4a.facade.usb.deviceids.Helpers.createTable;
import static org.qpython.qsl4a.qsl4a.facade.usb.deviceids.Helpers.createDevice;

public class CP2130Ids
{
    private static final long[] cp2130Devices = createTable(
            createDevice(0x10C4, 0x87a0)
    );

    public static boolean isDeviceSupported(int vendorId, int productId)
    {
        return Helpers.exists(cp2130Devices, vendorId, productId);
    }
}
