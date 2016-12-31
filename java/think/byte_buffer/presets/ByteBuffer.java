package think.byte_buffer.presets;

import org.bytedeco.javacpp.annotation.*;
import org.bytedeco.javacpp.tools.*;

@Properties(target="think.byte_buffer.ByteBuffer",
	    value={@Platform(include={"<byte_buffer.hpp>", "<byte_buffer_export.hpp>"},
			     includepath={"cpp"})})

public class ByteBuffer implements InfoMapper {
    public void map(InfoMap infoMap) {
    }
}
