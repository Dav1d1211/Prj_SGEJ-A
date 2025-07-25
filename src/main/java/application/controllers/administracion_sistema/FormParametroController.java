package application.controllers.administracion_sistema;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import application.dao.ParametroDAO;
import application.model.Parametro;

public class FormParametroController {

    @FXML private StackPane pnl_Title;
    @FXML private ToggleGroup toggleGroupEstado;
    @FXML private Label lbl_Titulo;
    @FXML private TextField txt_Codigo;
    @FXML private TextField txt_Nombre;
    @FXML private TextArea txt_Descripcion;
    @FXML private TextField txt_Valor;
    @FXML private ComboBox<String> cmb_Tipo;
    @FXML private ComboBox<String> cmb_Categoria;
    @FXML private RadioButton rb_Activo;
    @FXML private RadioButton rb_Inactivo;
    @FXML private Button btn_Guardar;
    @FXML private Button btn_Cancelar;

    // Componentes para upload de archivos
    @FXML private VBox vbox_Valor;
    @FXML private VBox vbox_Upload;
    @FXML private Button btn_SeleccionarArchivo;
    @FXML private Label lbl_NombreArchivo;
    @FXML private VBox vbox_Preview;
    @FXML private ImageView img_Preview;

    private ModuloParametrosController moduloParametrosController;
    private ModuloParametrosController.ParametroDemo parametroActual;
    private String accionActual;
    private File archivoSeleccionado;
    private String rutaArchivoDestino;

    public void setModuloParametrosController(ModuloParametrosController controller) {
        this.moduloParametrosController = controller;
    }

    @FXML
    private void initialize() {
        inicializarComboTipo();
        inicializarComboCategorias();
        configurarBotones();
        configurarValidaciones();
        configurarUploadArchivos();
    }

    private void inicializarComboTipo() {
        cmb_Tipo.getItems().clear();
        cmb_Tipo.getItems().addAll("TEXTO", "NUMERICO", "TIEMPO");
        cmb_Tipo.setValue("TEXTO");

        // Listener para mostrar/ocultar componentes de upload (si decides usar archivos)
        cmb_Tipo.valueProperty().addListener((observable, oldValue, newValue) -> {
            boolean esArchivo = "ARCHIVO".equalsIgnoreCase(newValue);
            vbox_Upload.setVisible(esArchivo);
            vbox_Upload.setManaged(esArchivo);
            vbox_Valor.setVisible(!esArchivo);
            vbox_Valor.setManaged(!esArchivo);
        });
    }

    private void inicializarComboCategorias() {
        cmb_Categoria.getItems().clear();
        cmb_Categoria.getItems().addAll(
            "General",
            "Sistema",
            "Institucional",
            "Legal/Fiscal",
            "Contable",
            "Seguridad"
        );
        cmb_Categoria.setValue("General");
    }

    private void configurarBotones() {
        btn_Guardar.setOnAction(e -> guardarParametro());
        btn_Cancelar.setOnAction(e -> cancelarOperacion());
    }

    private void configurarValidaciones() {
        // Validar que el código sea alfanumérico
        txt_Codigo.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.matches("[A-Za-z0-9]*")) {
                txt_Codigo.setText(oldValue);
            }
        });

        // Validar campos numéricos según el tipo seleccionado
        cmb_Tipo.valueProperty().addListener((observable, oldValue, newValue) -> {
            txt_Valor.textProperty().removeListener((obs, oldVal, newVal) -> {});
            if ("NUMERICO".equalsIgnoreCase(newValue)) {
                txt_Valor.textProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal != null && !newVal.matches("\\d*\\.?\\d*")) {
                        txt_Valor.setText(oldVal);
                    }
                });
            }
        });
    }

    public void inicializarFormulario(ModuloParametrosController.ParametroDemo parametro, String accion) {
        this.parametroActual = parametro;
        this.accionActual = accion;

        if ("NUEVO".equals(accion)) {
            lbl_Titulo.setText("Nuevo Parámetro");
            limpiarFormulario();
        } else if ("EDITAR".equals(accion) && parametro != null) {
            lbl_Titulo.setText("Editar Parámetro");
            cargarDatosParametro(parametro);
        }
    }

    private void limpiarFormulario() {
        txt_Codigo.clear();
        txt_Nombre.clear();
        txt_Descripcion.clear();
        txt_Valor.clear();
        cmb_Tipo.setValue("TEXTO");
        rb_Activo.setSelected(true);
        txt_Codigo.setEditable(true);
        txt_Codigo.setDisable(false);
        limpiarComponentesUpload();
    }

    private void cargarDatosParametro(ModuloParametrosController.ParametroDemo parametro) {
        txt_Codigo.setText(parametro.getCodigo());
        txt_Nombre.setText(parametro.getNombre());
        txt_Descripcion.setText(parametro.getDescripcion());
        txt_Valor.setText(parametro.getValor());

        // Ajusta el tipo para coincidir con el enum
        String tipo = parametro.getTipo().toUpperCase();
        if (tipo.equals("NUMERICO")) cmb_Tipo.setValue("NUMERICO");
        else if (tipo.equals("TEXTO")) cmb_Tipo.setValue("TEXTO");
        else if (tipo.equals("TIEMPO")) cmb_Tipo.setValue("TIEMPO");
        else cmb_Tipo.setValue("TEXTO");

        rb_Activo.setSelected(true);
        txt_Codigo.setEditable(false);
        txt_Codigo.setDisable(true);
    }

    private void guardarParametro() {
        if (!validarFormulario()) {
            return;
        }

        try {
            // Si es un archivo, copiarlo al destino (si decides usar archivos)
            if ("ARCHIVO".equalsIgnoreCase(cmb_Tipo.getValue()) && archivoSeleccionado != null) {
                copiarArchivoADestino(archivoSeleccionado);
            }

            // Construir objeto Parametro
            String tipo = cmb_Tipo.getValue().toUpperCase(); // Debe ser NUMERICO, TEXTO, TIEMPO
            String estado = rb_Activo.isSelected() ? "ACTIVO" : "INACTIVO";
            Parametro parametro = new Parametro(
                txt_Codigo.getText().trim(),
                txt_Nombre.getText().trim(),
                txt_Descripcion.getText().trim(),
                txt_Valor.getText().trim(),
                tipo,
                estado
            );

            ParametroDAO dao = new ParametroDAO();
            boolean exito;
            if ("NUEVO".equals(accionActual)) {
                exito = dao.insertarParametro(parametro);
            } else {
                exito = dao.actualizarParametro(parametro);
            }

            if (exito) {
                mostrarInfo("El parámetro se guardó exitosamente");
                moduloParametrosController.actualizarTabla();
                moduloParametrosController.cerrarFormulario();
            } else {
                mostrarError("No se pudo guardar el parámetro. Verifique que el código no esté repetido.");
            }
        } catch (Exception e) {
            mostrarError("Error al guardar el parámetro: " + e.getMessage());
        }
    }

    private boolean validarFormulario() {
        StringBuilder errores = new StringBuilder();

        if (txt_Codigo.getText() == null || txt_Codigo.getText().trim().isEmpty()) {
            errores.append("- El código es obligatorio\n");
        }

        if (txt_Nombre.getText() == null || txt_Nombre.getText().trim().isEmpty()) {
            errores.append("- El nombre es obligatorio\n");
        }

        // Validar valor solo si no es tipo archivo
        if (!"ARCHIVO".equalsIgnoreCase(cmb_Tipo.getValue())) {
            if (txt_Valor.getText() == null || txt_Valor.getText().trim().isEmpty()) {
                errores.append("- El valor es obligatorio\n");
            }
        } else {
            // Para archivos, validar que se haya seleccionado un archivo
            if (archivoSeleccionado == null) {
                errores.append("- Debe seleccionar un archivo\n");
            }
        }

        if (cmb_Tipo.getValue() == null) {
            errores.append("- El tipo es obligatorio\n");
        }

        // Validaciones específicas por tipo
        if (cmb_Tipo.getValue() != null && txt_Valor.getText() != null) {
            String tipo = cmb_Tipo.getValue().toUpperCase();
            String valor = txt_Valor.getText().trim();

            if ("NUMERICO".equals(tipo) && !valor.matches("\\d+(\\.\\d+)?")) {
                errores.append("- El valor debe ser numérico\n");
            }
        }

        if (errores.length() > 0) {
            mostrarError("Por favor corrija los siguientes errores:\n\n" + errores.toString());
            return false;
        }

        return true;
    }

    private void cancelarOperacion() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar");
        alert.setHeaderText("¿Desea cancelar la operación?");
        alert.setContentText("Se perderán todos los cambios realizados");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            limpiarComponentesUpload();
            moduloParametrosController.cerrarFormulario();
        }
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarInfo(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Información");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void configurarUploadArchivos() {
        btn_SeleccionarArchivo.setOnAction(e -> seleccionarArchivo());
        btn_SeleccionarArchivo.getStyleClass().add("upload-button");
    }

    private void seleccionarArchivo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar archivo");

        // Configurar filtros según el tipo de parámetro
        String categoria = cmb_Categoria.getValue();
        if ("Institucional".equals(categoria)) {
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                new FileChooser.ExtensionFilter("Documentos", "*.pdf", "*.doc", "*.docx"),
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*")
            );
        } else {
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*"),
                new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                new FileChooser.ExtensionFilter("Documentos", "*.pdf", "*.doc", "*.docx", "*.txt"),
                new FileChooser.ExtensionFilter("Archivos de configuración", "*.xml", "*.json", "*.properties")
            );
        }

        // Obtener la ventana padre
        Stage stage = (Stage) btn_SeleccionarArchivo.getScene().getWindow();
        File archivo = fileChooser.showOpenDialog(stage);

        if (archivo != null) {
            archivoSeleccionado = archivo;
            lbl_NombreArchivo.setText(archivo.getName());
            lbl_NombreArchivo.getStyleClass().add("file-selected-label");

            // Mostrar preview si es imagen
            if (esImagen(archivo)) {
                mostrarPreviewImagen(archivo);
            }

            // Establecer el valor del campo como la ruta del archivo
            txt_Valor.setText(archivo.getAbsolutePath());
        }
    }

    private boolean esImagen(File archivo) {
        String nombre = archivo.getName().toLowerCase();
        return nombre.endsWith(".png") || nombre.endsWith(".jpg") ||
               nombre.endsWith(".jpeg") || nombre.endsWith(".gif") ||
               nombre.endsWith(".bmp");
    }

    private void mostrarPreviewImagen(File archivo) {
        try {
            FileInputStream fis = new FileInputStream(archivo);
            Image image = new Image(fis);
            img_Preview.setImage(image);
            vbox_Preview.setVisible(true);
            vbox_Preview.setManaged(true);
            fis.close();
        } catch (IOException e) {
            System.err.println("Error al cargar la imagen: " + e.getMessage());
        }
    }

    private void copiarArchivoADestino(File archivoOrigen) throws IOException {
        if (archivoOrigen == null) return;

        // Crear directorio de destino si no existe
        String directorioDestino = "src/main/resources/uploads/";
        Path rutaDestino = Paths.get(directorioDestino);
        if (!Files.exists(rutaDestino)) {
            Files.createDirectories(rutaDestino);
        }

        // Generar nombre único para el archivo
        String nombreArchivo = txt_Codigo.getText() + "_" + archivoOrigen.getName();
        Path archivoDestino = rutaDestino.resolve(nombreArchivo);

        // Copiar archivo
        Files.copy(archivoOrigen.toPath(), archivoDestino, StandardCopyOption.REPLACE_EXISTING);

        // Actualizar la ruta de destino
        rutaArchivoDestino = archivoDestino.toString();
        txt_Valor.setText(rutaArchivoDestino);
    }

    private void limpiarComponentesUpload() {
        archivoSeleccionado = null;
        rutaArchivoDestino = null;
        lbl_NombreArchivo.setText("Ningún archivo seleccionado");
        lbl_NombreArchivo.getStyleClass().remove("file-selected-label");
        img_Preview.setImage(null);
        vbox_Preview.setVisible(false);
        vbox_Preview.setManaged(false);
    }
}