import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomInputStream.IncludeBulkData;
import org.dcm4che3.io.DicomOutputStream;
import org.dcm4che3.json.JSONReader;
import org.dcm4che3.json.JSONWriter;
import org.dcm4che3.util.TagUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;

import javax.json.Json;
import javax.json.stream.JsonGenerator;

/**
 * @author Miguel Azevedo
 * OverlayManagement: provides methods for detaching,
 * jsonifying and attaching Overlay related attributes to DICOM files.
 */
public class OverlayManagement {

    /**
     * Detaches Overlay related attrubutes(60xx,yyyy tags) from a given file and jsonifyes it.
     * Does not write changes to any file.
     * @param streamIn
     * @return The json encoded Overlay data String detached from streamIn.
     */
    public static String detachOverlayJSON(DicomInputStream streamIn) {
        return detachOverlayJSON(streamIn, null);
    }

    /**
     * Detaches Overlay data(60xx,yyyy tags) from a given file and jsonifyes it.
     * Writes the remaining attributes to streamOut.
     * @param streamIn
     * @param streamOut
     * @return The json encoded Overlay data String detached from streamIn.
     */
    public static String detachOverlayJSON(DicomInputStream streamIn, DicomOutputStream streamOut) {
        //reading DICOM Object's attibutes
        Attributes remainAttribs;
        Attributes mData;
        try {
            streamIn.setIncludeBulkData(IncludeBulkData.YES);
            streamIn.setAddBulkDataReferences(true);
            mData = streamIn.readFileMetaInformation();
            remainAttribs = streamIn.readDataset(-1, -1);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }

        Attributes overlayAttribs = new Attributes();

        //Extracting all Overlay-related attributes
        int tags[] = remainAttribs.tags();
        for (int i : tags) {
            if (TagUtils.groupNumber(Tag.OverlayColumns) == TagUtils.groupNumber(i)) {
                overlayAttribs.addSelected(remainAttribs, i);
                remainAttribs.remove(i);
            }
        }
        //Jsonifying Overlay-related attributes
        StringWriter writer = new StringWriter();
        JsonGenerator gen = Json.createGenerator(writer);
        new JSONWriter(gen).write(overlayAttribs);
        gen.flush();
        String json = writer.toString();

        if (streamOut != null) {
            //Write file with no overlays
            try {
                streamOut.writeDataset(mData,remainAttribs);
                streamOut.finish();
                streamOut.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return json;
    }

    /**
     * Returns Overlay related attributes read from the json String.
     * @param jsonOverlay
     * @return
     */
    public static Attributes addOverlayJSON(String jsonOverlay) {
        return addOverlayJSON(jsonOverlay, null, null);
    }

    /**
     * Returns Overlay related attributes read from the json String
     * along with all the remaining attributes from streamIn.
     * @param jsonOverlay
     * @param streamIn
     * @return
     */
    public static Attributes addOverlayJSON(String jsonOverlay, DicomInputStream streamIn) {
        return addOverlayJSON(jsonOverlay, streamIn, null);
    }

    /**
     * Returns Overlay related attributes read from the json String
     * along with all the remaining attributes from streamIn.
     * Writes streamIn with the Overlay related attributes on streamOut.
     * @param jsonOverlay
     * @param streamIn
     * @param streamOut
     * @return
     */
    public static Attributes addOverlayJSON(String jsonOverlay,
                                     DicomInputStream streamIn,
                                     DicomOutputStream streamOut) {

        Attributes overlayAttribs = new Attributes();
        Attributes remainAttribs;
        Attributes mData;

        JSONReader reader = new JSONReader(
                Json.createParser(new ByteArrayInputStream(jsonOverlay.getBytes(StandardCharsets.UTF_8))));
        reader.readDataset(overlayAttribs);

        if (streamIn != null) {
            //Reading attributes from input file.
            try {
                streamIn.setIncludeBulkData(IncludeBulkData.YES);
                streamIn.setAddBulkDataReferences(true);
                mData = streamIn.readFileMetaInformation();
                remainAttribs = streamIn.readDataset(-1, -1);
            } catch (IOException e) {
                e.printStackTrace();
                return overlayAttribs;
            }
            remainAttribs.addAll(overlayAttribs);

            //Writing file with no overlays
            if (streamOut != null) {
                try {
                    streamOut.writeDataset(mData, remainAttribs);
                    streamOut.finish();
                    streamOut.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            remainAttribs.addAll(mData);
            return remainAttribs;
        }
        return overlayAttribs;
    }
}
