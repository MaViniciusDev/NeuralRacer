module org.MaViniciusDev {
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.fxml;

    opens org.MaViniciusDev.main to javafx.graphics;
    exports org.MaViniciusDev.view;
    exports org.MaViniciusDev.main;
}
