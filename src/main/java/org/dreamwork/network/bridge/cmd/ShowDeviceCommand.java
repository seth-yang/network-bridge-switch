package org.dreamwork.network.bridge.cmd;

import org.dreamwork.cli.text.Alignment;
import org.dreamwork.cli.text.TextFormater;
import org.dreamwork.network.bridge.Context;
import org.dreamwork.network.bridge.data.Device;
import org.dreamwork.telnet.Console;
import org.dreamwork.telnet.command.Command;

import java.io.IOException;
import java.util.List;

/**
 * Created by seth.yang on 2019/11/6
 */
public class ShowDeviceCommand extends Command {
    private static final String[] HEADER = {"ID", "Name", "Address", "Port", "User"};
    private static final int ID = 0, NAME = 1, ADDRESS = 2, PORT = 3, USER = 4;

    public ShowDeviceCommand () {
        super ("show-device", "sd", "show all registered devices");
    }

    @Override
    public void perform (Console console) throws IOException {
        List<Device> devices = Context.db.get (Device.class, null, "name asc");
        int[] widthes = new int[HEADER.length];
        for (int i = 0; i < HEADER.length; i ++) {
            widthes[i] = HEADER[i].length ();
        }
        for (Device device : devices) {
            String id = String.valueOf (device.getId ());
            if (id.length () > widthes [ID]) {
                widthes [ID] = id.length ();
            }
            if (device.getName ().length () > widthes[NAME]) {
                widthes [NAME] = device.getName ().length ();
            }
            if (device.getHost ().length () > widthes[ADDRESS]) {
                widthes [ADDRESS] = device.getHost ().length ();
            }
            String port = String.valueOf (device.getPort ());
            if (port.length () > widthes[PORT]) {
                widthes [PORT] = port.length ();
            }
        }

        // write the header
        console.write ("    ");
        console.write (TextFormater.fill (HEADER[ID], ' ', widthes[ID], Alignment.Right));
        console.write ("    ");
        console.write (TextFormater.fill (HEADER[NAME], ' ', widthes[NAME], Alignment.Left));
        console.write ("    ");
        console.write (TextFormater.fill (HEADER[ADDRESS], ' ', widthes[ADDRESS], Alignment.Left));
        console.write ("    ");
        console.write (TextFormater.fill (HEADER[PORT], ' ', widthes[PORT], Alignment.Right));
        console.write ("    ");
        console.println (HEADER[USER]);

        // write the data
        for (Device device : devices) {
            String id   = String.valueOf (device.getId ());
            String port = String.valueOf (device.getPort ());
            console.write ("    ");
            console.write (TextFormater.fill (id, ' ', widthes[ID], Alignment.Right));
            console.write ("    ");
            console.write (TextFormater.fill (device.getName (), ' ', widthes[NAME], Alignment.Left));
            console.write ("    ");
            console.write (TextFormater.fill (device.getHost (), ' ', widthes[ADDRESS], Alignment.Left));
            console.write ("    ");
            console.write (TextFormater.fill (port, ' ', widthes[PORT], Alignment.Right));
            console.write ("    ");
            console.println (device.getUser ());
        }
        console.println ();
    }
}
