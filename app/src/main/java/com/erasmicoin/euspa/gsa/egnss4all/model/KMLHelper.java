package com.erasmicoin.euspa.gsa.egnss4all.model;

import android.os.ParcelFileDescriptor;
import android.util.Xml;

import com.erasmicoin.euspa.gsa.egnss4all.model.pathTrack.PTPath;
import com.erasmicoin.euspa.gsa.egnss4all.model.pathTrack.PTPoint;

import org.xmlpull.v1.XmlSerializer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class KMLHelper {


    public static FileOutputStream writeKML(PTPath path, ParcelFileDescriptor pfd) {


        FileOutputStream fileos = null;
        fileos = new FileOutputStream(pfd.getFileDescriptor());

        XmlSerializer xmlSerializer = Xml.newSerializer();
        try {
            xmlSerializer.setOutput(fileos, "UTF-8");

            xmlSerializer.setOutput(fileos, "UTF-8");
            xmlSerializer.startDocument(null, null);
            xmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            xmlSerializer.startTag(null, "kml");
            xmlSerializer.startTag(null, "Document");
            xmlSerializer.startTag(null, "name");
            xmlSerializer.text(path.getName());
            xmlSerializer.endTag(null, "name");
            xmlSerializer.startTag(null, "Style");
            xmlSerializer.attribute(null, "id", "transGreenPoly");
            xmlSerializer.startTag(null, "LineStyle");
            xmlSerializer.startTag(null, "width");
            xmlSerializer.text("1");
            xmlSerializer.endTag(null, "width");
            xmlSerializer.startTag(null, "color");
            xmlSerializer.text("ff0000");
            xmlSerializer.endTag(null, "color");
            xmlSerializer.startTag(null, "colorMode");
            xmlSerializer.text("random");
            xmlSerializer.endTag(null, "colorMode");
            xmlSerializer.endTag(null, "LineStyle");
            xmlSerializer.endTag(null, "Style");
            xmlSerializer.startTag(null, "Folder");
            xmlSerializer.startTag(null, "name");
            xmlSerializer.text(path.getName());
            xmlSerializer.endTag(null, "name");
            xmlSerializer.startTag(null, "visibility");
            xmlSerializer.text("1");
            xmlSerializer.endTag(null, "visibility");
            xmlSerializer.startTag(null, "description");
            xmlSerializer.text(path.getDeviceManufacture()+" "+path.getDeviceModel()+" "+path.getDevicePlatform()+" "+path.getDeviceVersion());
            xmlSerializer.endTag(null, "description");
            xmlSerializer.startTag(null, "Placemark");
            xmlSerializer.startTag(null, "name");
            xmlSerializer.text(path.getName());
            xmlSerializer.endTag(null, "name");
            xmlSerializer.startTag(null, "visibility");
            xmlSerializer.text("1");
            xmlSerializer.endTag(null, "visibility");
            xmlSerializer.startTag(null, "styleUrl");
            xmlSerializer.text("#transRedPoly");
            xmlSerializer.endTag(null, "styleUrl");
            xmlSerializer.startTag(null, "LineString");
            xmlSerializer.startTag(null, "extrude");
            xmlSerializer.text("1");
            xmlSerializer.endTag(null, "extrude");
            xmlSerializer.startTag(null, "altitudeMode");
            xmlSerializer.text("relativeToGround");
            xmlSerializer.endTag(null, "altitudeMode");
            xmlSerializer.startTag(null, "coordinates");

            List<PTPoint> points = path.getPoints();
            for (PTPoint point : points) {
                xmlSerializer.text(point.getLongitude() + "," + point.getLatitude() + ",0 \n");
            }

            xmlSerializer.endTag(null, "coordinates");
            xmlSerializer.endTag(null, "LineString");
            xmlSerializer.endTag(null, "Placemark");
            xmlSerializer.endTag(null, "Folder");
            xmlSerializer.endTag(null, "Document");
            xmlSerializer.endTag(null, "kml");
            xmlSerializer.endDocument();
            xmlSerializer.flush();
            //fileos.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return fileos;
    }
}
