module qupath.extension.pathscope {

    requires okhttp3;
    requires com.google.gson;

    requires java.net.http; // For HTTP client if needed
    requires java.desktop; // For BufferedImage
    requires javafx.controls;
    requires org.slf4j;
    requires org.controlsfx.controls;

    provides qupath.lib.gui.extensions.QuPathExtension with
            qupath.extension.pathscope.ui.ApiIntegrationExtension;
}
