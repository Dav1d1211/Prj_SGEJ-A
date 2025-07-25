package application.controllers.cliente;

import application.model.Cliente;
import application.service.ClienteService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ModuloClienteController {

    @FXML
    private Button btn_Nuevo;
    @FXML
    private Button btn_Buscar;
    @FXML
    private Button btn_LimpiarFiltro;
    @FXML
    private TextField txt_Busqueda;
    @FXML
    private Label lbl_TotalClientes;

    // Filtro de estado (solo para administradores)
    @FXML
    private VBox vbox_FiltroEstado;
    @FXML
    private ComboBox<String> cmb_FiltroEstado;

    @FXML
    private TableView<Cliente> tb_Clientes;
    @FXML
    private TableColumn<Cliente, String> tbc_Nombres;
    @FXML
    private TableColumn<Cliente, String> tbc_NumeroI;
    @FXML
    private TableColumn<Cliente, String> tbc_TipoIdentificacion;
    @FXML
    private TableColumn<Cliente, String> tbc_Telefono;
    @FXML
    private TableColumn<Cliente, String> tbc_Correo;
    @FXML
    private TableColumn<Cliente, String> tbc_Estado;

    @FXML
    private TableColumn<Cliente, Void> tbc_BotonEditar;
    @FXML
    private TableColumn<Cliente, Void> tbc_BotonVer;

    private Pane pnl_Forms;
    private ClienteService clienteService;
    private String tipoUsuario; // Para saber si es administrador

    // Flag para evitar mÃºltiples ejecuciones
    private boolean formularioAbierto = false;

    // Variables para paginaciÃ³n
    private int paginaActual = 1;
    private int registrosPorPagina = 10;
    private int totalRegistros = 0;
    private int totalPaginas = 0;
    private ObservableList<Cliente> todosLosClientes = FXCollections.observableArrayList();

    // Elementos de paginaciÃ³n del FXML
    @FXML
    private Label lbl_InfoPaginacion;
    @FXML
    private Label lbl_PaginaActual;
    @FXML
    private Label lbl_TotalPaginas;
    @FXML
    private TextField txt_PaginaActual;
    @FXML
    private Button btn_PrimeraPagina;
    @FXML
    private Button btn_PaginaAnterior;
    @FXML
    private Button btn_PaginaSiguiente;
    @FXML
    private Button btn_UltimaPagina;
    @FXML
    private ComboBox<Integer> cmb_RegistrosPorPagina;

    public void setFormularioContainer(Pane pnl_Forms) {
        this.pnl_Forms = pnl_Forms;
    }

    @FXML
    private void initialize() {
        clienteService = new ClienteService();

        btn_Nuevo.setOnAction(_ -> mostrarFormulario(null, "NUEVO"));
        btn_Buscar.setOnAction(_ -> buscarClientes());
        btn_LimpiarFiltro.setOnAction(_ -> limpiarFiltro());

        // Configurar ComboBox de filtro
        configurarFiltroEstado();

        configurarColumnasTexto();
        inicializarColumnasDeBotones();
        cargarClientesDesdeBaseDatos();
        ocultarEncabezadosColumnasDeAccion();
        actualizarContadorClientes();

        tbc_BotonEditar.getStyleClass().add("column-action");
        tbc_BotonVer.getStyleClass().add("column-action");

        // Configurar paginaciÃ³n
        configurarPaginacion();
    }

    private void ocultarEncabezadosColumnasDeAccion() {
        tb_Clientes.widthProperty().addListener((_, _, _) -> {
            double anchoOcupadoPorColumnasVisibles = tbc_Nombres.getWidth() + tbc_NumeroI.getWidth()
                    + tbc_TipoIdentificacion.getWidth() + tbc_Telefono.getWidth() + tbc_Correo.getWidth()
                    + tbc_Estado.getWidth();
            double anchoDispobibleParaColumnaDeAccion = tb_Clientes.getWidth() - anchoOcupadoPorColumnasVisibles;
            double anchoParaCadaColumnaDeAccion = anchoDispobibleParaColumnaDeAccion / 2;
            tbc_BotonEditar.setPrefWidth(anchoParaCadaColumnaDeAccion);
            tbc_BotonVer.setPrefWidth(anchoParaCadaColumnaDeAccion);
        });
    }

    private void configurarColumnasTexto() {
        tbc_Nombres.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNombreCompleto()));
        tbc_NumeroI.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNumeroIdentificacion()));
        tbc_TipoIdentificacion
                .setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTipoIdentificacion().name()));
        tbc_Telefono.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTelefono()));
        tbc_Correo.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCorreoElectronico()));
        tbc_Estado.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEstado().name()));
    }

    private void inicializarColumnasDeBotones() {
        agregarBotonPorColumna(tbc_BotonEditar, "âœŽ", "Editar");
        agregarBotonPorColumna(tbc_BotonVer, "ðŸ‘�", "Ver");

        // Eliminar encabezados de las columnas de acciones
        tbc_BotonEditar.setText("");
        tbc_BotonVer.setText("");

        tbc_BotonEditar.setPrefWidth(40);
        tbc_BotonVer.setPrefWidth(40);
    }

    private void agregarBotonPorColumna(TableColumn<Cliente, Void> columna, String texto, String tooltip) {
        columna.getStyleClass().add("column-action");

        columna.setCellFactory(_ -> new TableCell<>() {
            private final Button btn = new Button(texto);

            {
                // Estilos mejorados para botones mÃ¡s compactos y profesionales
                btn.getStyleClass().add("table-button");
                setStyle("-fx-alignment: CENTER; -fx-padding: 2;");
                btn.setTooltip(new Tooltip(tooltip));

                // Estilos especÃ­ficos segÃºn el tipo de botÃ³n
                if ("Editar".equals(tooltip)) {
                    btn.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-background-radius: 4; " +
                            "-fx-font-size: 10px; -fx-font-weight: bold; -fx-min-width: 55; -fx-max-width: 55; " +
                            "-fx-min-height: 25; -fx-max-height: 25; -fx-cursor: hand; -fx-padding: 0;");
                } else if ("Ver".equals(tooltip)) {
                    btn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-background-radius: 4; " +
                            "-fx-font-size: 10px; -fx-font-weight: bold; -fx-min-width: 55; -fx-max-width: 55; " +
                            "-fx-min-height: 25; -fx-max-height: 25; -fx-cursor: hand; -fx-padding: 0;");
                }

                // Efectos hover
                btn.setOnMouseEntered(_ -> {
                    if ("Editar".equals(tooltip)) {
                        btn.setStyle(btn.getStyle() + "-fx-background-color: #d97706;");
                    } else if ("Ver".equals(tooltip)) {
                        btn.setStyle(btn.getStyle() + "-fx-background-color: #2563eb;");
                    }
                });

                btn.setOnMouseExited(_ -> {
                    if ("Editar".equals(tooltip)) {
                        btn.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-background-radius: 4; " +
                                "-fx-font-size: 10px; -fx-font-weight: bold; -fx-min-width: 55; -fx-max-width: 55; " +
                                "-fx-min-height: 25; -fx-max-height: 25; -fx-cursor: hand; -fx-padding: 0;");
                    } else if ("Ver".equals(tooltip)) {
                        btn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-background-radius: 4; " +
                                "-fx-font-size: 10px; -fx-font-weight: bold; -fx-min-width: 55; -fx-max-width: 55; " +
                                "-fx-min-height: 25; -fx-max-height: 25; -fx-cursor: hand; -fx-padding: 0;");
                    }
                });

                btn.setOnAction(_ -> {
                    Cliente cliente = getTableView().getItems().get(getIndex());
                    if ("Editar".equals(tooltip)) {
                        mostrarFormulario(cliente, "EDITAR");
                    } else if ("Ver".equals(tooltip)) {
                        mostrarFormulario(cliente, "VER");
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    /**
     * Cargar clientes desde la base de datos (considera filtro de estado para
     * administradores)
     */
    private void cargarClientesDesdeBaseDatos() {
        try {
            // Cargar todos los clientes segÃºn filtros
            if ("Administrador".equals(tipoUsuario) && cmb_FiltroEstado != null && cmb_FiltroEstado.isVisible()) {
                String filtroEstado = cmb_FiltroEstado.getSelectionModel().getSelectedItem();

                if ("Todos".equals(filtroEstado)) {
                    todosLosClientes = clienteService.obtenerTodosLosClientesIncluirInactivos();
                } else if ("Inactivos".equals(filtroEstado)) {
                    todosLosClientes = clienteService.obtenerClientesPorEstado(Cliente.Estado.INACTIVO);
                } else {
                    todosLosClientes = clienteService.obtenerClientesActivos();
                }
            } else {
                todosLosClientes = clienteService.obtenerClientesActivos();
            }

            totalRegistros = todosLosClientes.size();
            calcularTotalPaginas();
            mostrarPaginaActual();
            actualizarInfoPaginacion();
            actualizarEstadoBotones();

        } catch (Exception e) {
            System.err.println("Error al cargar clientes: " + e.getMessage());
            e.printStackTrace();
            todosLosClientes = FXCollections.observableArrayList();
            totalRegistros = 0;
            mostrarPaginaActual();
            actualizarInfoPaginacion();
        }
    }

    /**
     * Mostrar solo los registros de la pÃ¡gina actual
     */
    private void mostrarPaginaActual() {
        int inicio = (paginaActual - 1) * registrosPorPagina;
        int fin = Math.min(inicio + registrosPorPagina, totalRegistros);

        ObservableList<Cliente> clientesPagina = FXCollections.observableArrayList();

        if (inicio < totalRegistros) {
            for (int i = inicio; i < fin; i++) {
                clientesPagina.add(todosLosClientes.get(i));
            }
        }

        tb_Clientes.setItems(clientesPagina);
        actualizarContadorClientes();
    }

    /**
     * Calcular total de pÃ¡ginas
     */
    private void calcularTotalPaginas() {
        totalPaginas = (int) Math.ceil((double) totalRegistros / registrosPorPagina);
        if (totalPaginas == 0)
            totalPaginas = 1;
    }

    /**
     * Actualizar informaciÃ³n de paginaciÃ³n
     */
    private void actualizarInfoPaginacion() {
        int inicio = totalRegistros > 0 ? (paginaActual - 1) * registrosPorPagina + 1 : 0;
        int fin = Math.min(paginaActual * registrosPorPagina, totalRegistros);

        if (lbl_InfoPaginacion != null) {
            lbl_InfoPaginacion.setText(inicio + "-" + fin + " de " + totalRegistros);
        }

        if (lbl_PaginaActual != null) {
            lbl_PaginaActual.setText("PÃ¡gina " + paginaActual + " de " + totalPaginas);
        }

        if (lbl_TotalPaginas != null) {
            lbl_TotalPaginas.setText(String.valueOf(totalPaginas));
        }

        if (txt_PaginaActual != null) {
            txt_PaginaActual.setText(String.valueOf(paginaActual));
        }
    }

    /**
     * Actualizar estado de botones de navegaciÃ³n
     */
    private void actualizarEstadoBotones() {
        if (btn_PrimeraPagina != null) {
            btn_PrimeraPagina.setDisable(paginaActual <= 1);
        }
        if (btn_PaginaAnterior != null) {
            btn_PaginaAnterior.setDisable(paginaActual <= 1);
        }
        if (btn_PaginaSiguiente != null) {
            btn_PaginaSiguiente.setDisable(paginaActual >= totalPaginas);
        }
        if (btn_UltimaPagina != null) {
            btn_UltimaPagina.setDisable(paginaActual >= totalPaginas);
        }
    }

    // MÃ©todos de navegaciÃ³n
    private void irAPrimeraPagina() {
        paginaActual = 1;
        mostrarPaginaActual();
        actualizarInfoPaginacion();
        actualizarEstadoBotones();
    }

    private void irAPaginaAnterior() {
        if (paginaActual > 1) {
            paginaActual--;
            mostrarPaginaActual();
            actualizarInfoPaginacion();
            actualizarEstadoBotones();
        }
    }

    private void irAPaginaSiguiente() {
        if (paginaActual < totalPaginas) {
            paginaActual++;
            mostrarPaginaActual();
            actualizarInfoPaginacion();
            actualizarEstadoBotones();
        }
    }

    private void irAUltimaPagina() {
        paginaActual = totalPaginas;
        mostrarPaginaActual();
        actualizarInfoPaginacion();
        actualizarEstadoBotones();
    }

    private void irAPaginaEspecifica() {
        try {
            int nuevaPagina = Integer.parseInt(txt_PaginaActual.getText());
            if (nuevaPagina >= 1 && nuevaPagina <= totalPaginas) {
                paginaActual = nuevaPagina;
                mostrarPaginaActual();
                actualizarInfoPaginacion();
                actualizarEstadoBotones();
            } else {
                txt_PaginaActual.setText(String.valueOf(paginaActual));
            }
        } catch (NumberFormatException e) {
            txt_PaginaActual.setText(String.valueOf(paginaActual));
        }
    }

    private void cambiarRegistrosPorPagina() {
        registrosPorPagina = cmb_RegistrosPorPagina.getValue();
        paginaActual = 1; // Volver a la primera pÃ¡gina
        calcularTotalPaginas();
        mostrarPaginaActual();
        actualizarInfoPaginacion();
        actualizarEstadoBotones();
    }

    /**
     * Buscar clientes con paginaciÃ³n
     */
    private void buscarClientes() {
        String textoBusqueda = txt_Busqueda.getText().trim();

        try {
            if (textoBusqueda.isEmpty()) {
                cargarClientesDesdeBaseDatos();
                return;
            }

            // Buscar en todos los clientes segÃºn filtros
            if ("Administrador".equals(tipoUsuario) && cmb_FiltroEstado != null && cmb_FiltroEstado.isVisible()) {
                String filtroEstado = cmb_FiltroEstado.getSelectionModel().getSelectedItem();

                if ("Todos".equals(filtroEstado)) {
                    todosLosClientes = clienteService.buscarClientesPorNombre(textoBusqueda);
                } else if ("Inactivos".equals(filtroEstado)) {
                    todosLosClientes = clienteService.buscarClientesPorNombre(textoBusqueda);
                    todosLosClientes = todosLosClientes
                            .filtered(cliente -> cliente.getEstado() == Cliente.Estado.INACTIVO);
                } else {
                    todosLosClientes = clienteService.buscarClientesActivosPorNombre(textoBusqueda);
                }
            } else {
                todosLosClientes = clienteService.buscarClientesActivosPorNombre(textoBusqueda);
            }

            totalRegistros = todosLosClientes.size();
            paginaActual = 1; // Volver a la primera pÃ¡gina
            calcularTotalPaginas();
            mostrarPaginaActual();
            actualizarInfoPaginacion();
            actualizarEstadoBotones();

        } catch (Exception e) {
            System.err.println("Error al buscar clientes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Mostrar formulario de cliente
     */
    private void mostrarFormulario(Cliente cliente, String modo) {
        if (formularioAbierto) {
            System.out.println("ðŸ”§ Formulario ya estÃ¡ abierto, ignorando solicitud");
            return;
        }

        try {
            formularioAbierto = true;
            System.out.println("ðŸ”§ Iniciando mostrarFormulario con modo: " + modo);
            System.out.println("ðŸ”§ Cliente: " + (cliente != null ? cliente.getNombreCompleto() : "null"));
            System.out.println("ðŸ”§ Panel Forms: " + (pnl_Forms != null ? "OK" : "NULL"));

            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/views/cliente/form_cliente.fxml"));
            Node nodo = fxmlLoader.load();
            FormClienteController controller = fxmlLoader.getController();

            controller.setModo(modo);

            if (cliente != null) {
                controller.cargarCliente(cliente);
            }

            // Configurar callbacks para refrescar la tabla
            controller.setOnGuardar(() -> {
                System.out.println("ðŸ”§ Callback onGuardar ejecutado");
                cargarClientesDesdeBaseDatos();
                formularioAbierto = false;
                if (pnl_Forms != null) {
                    pnl_Forms.getChildren().clear();
                    pnl_Forms.setVisible(false);
                    pnl_Forms.setManaged(false);
                }
            });

            controller.setOnCancelar(() -> {
                System.out.println("ðŸ”§ Callback onCancelar ejecutado");
                formularioAbierto = false;
                if (pnl_Forms != null) {
                    pnl_Forms.getChildren().clear();
                    pnl_Forms.setVisible(false);
                    pnl_Forms.setManaged(false);
                }
            });

            if (pnl_Forms != null) {
                // Hacer visible el panel de formularios
                pnl_Forms.setVisible(true);
                pnl_Forms.setManaged(true);

                pnl_Forms.getChildren().clear();
                pnl_Forms.getChildren().add(nodo);
                AnchorPane.setTopAnchor(nodo, 0.0);
                AnchorPane.setBottomAnchor(nodo, 0.0);
                AnchorPane.setLeftAnchor(nodo, 0.0);
                AnchorPane.setRightAnchor(nodo, 0.0);
                System.out.println("ðŸ”§ Formulario cargado exitosamente");
            } else {
                // Si no hay panel de formularios, abrir en ventana nueva
                System.out.println("ðŸ”§ Abriendo formulario en ventana nueva");
                Stage stage = new Stage();
                Scene scene = new Scene((Parent) nodo);

                // Agregar estilos CSS
                scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());

                stage.setScene(scene);
                stage.setTitle("Cliente - " + modo);
                stage.initModality(Modality.WINDOW_MODAL);

                // Configurar evento de cierre
                stage.setOnHiding(_ -> formularioAbierto = false);

                stage.show();
            }
        } catch (IOException e) {
            formularioAbierto = false;
            System.err.println("ðŸ”§ ERROR al cargar formulario: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            formularioAbierto = false;
            System.err.println("ðŸ”§ ERROR general: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * MÃ©todo pÃºblico para refrescar la tabla desde otros controladores
     */
    public void refrescarTabla() {
        cargarClientesDesdeBaseDatos();
    }

    /**
     * MÃ©todo para resetear el estado del formulario
     */
    public void resetearFormulario() {
        formularioAbierto = false;
        if (pnl_Forms != null) {
            pnl_Forms.getChildren().clear();
            pnl_Forms.setVisible(false);
            pnl_Forms.setManaged(false);
        }
    }

    /**
     * Limpiar filtro de bÃºsqueda y mostrar todos los clientes
     */
    private void limpiarFiltro() {
        txt_Busqueda.clear();
        // Resetear el filtro de estado si es administrador
        if (cmb_FiltroEstado != null && cmb_FiltroEstado.isVisible()) {
            cmb_FiltroEstado.getSelectionModel().selectFirst(); // Seleccionar "Todos"
        }
        cargarClientesDesdeBaseDatos();
    }

    /**
     * Configurar el ComboBox de filtro de estado
     */
    private void configurarFiltroEstado() {
        if (cmb_FiltroEstado != null) {
            // Limpiar items existentes para evitar duplicados
            cmb_FiltroEstado.getItems().clear();
            cmb_FiltroEstado.getItems().addAll("Todos", "Activos", "Inactivos");

            // Para administradores, seleccionar "Todos" por defecto
            if ("Administrador".equals(tipoUsuario)) {
                cmb_FiltroEstado.getSelectionModel().selectFirst(); // Seleccionar "Todos"
            } else {
                cmb_FiltroEstado.getSelectionModel().select("Activos"); // Seleccionar "Activos" para otros
            }

            // Listener para cuando cambie la selecciÃ³n
            cmb_FiltroEstado.getSelectionModel().selectedItemProperty().addListener((_, _, newValue) -> {
                if (newValue != null) {
                    filtrarPorEstado(newValue);
                }
            });
        }
    }

    /**
     * Filtrar clientes por estado
     */
    private void filtrarPorEstado(String filtro) {
        ObservableList<Cliente> clientesFiltrados;

        switch (filtro) {
            case "Activos":
                clientesFiltrados = clienteService.obtenerClientesActivos();
                break;
            case "Inactivos":
                clientesFiltrados = clienteService.obtenerClientesPorEstado(Cliente.Estado.INACTIVO);
                break;
            case "Todos":
            default:
                clientesFiltrados = clienteService.obtenerTodosLosClientesIncluirInactivos();
                break;
        }

        tb_Clientes.setItems(clientesFiltrados);
        actualizarContadorClientes();
    }

    /**
     * Configurar el mÃ³dulo segÃºn el tipo de usuario
     */
    public void configurarPorRol(String tipoUsuario) {
        this.tipoUsuario = tipoUsuario;

        // Mostrar filtro de estado solo para administradores
        if ("Administrador".equals(tipoUsuario)) {
            if (vbox_FiltroEstado != null) {
                vbox_FiltroEstado.setVisible(true);
                vbox_FiltroEstado.setManaged(true);
            }
            // Reconfigurar el ComboBox con el rol actualizado
            configurarFiltroEstado();
        } else {
            if (vbox_FiltroEstado != null) {
                vbox_FiltroEstado.setVisible(false);
                vbox_FiltroEstado.setManaged(false);
            }
        }

        // Recargar datos despuÃ©s de configurar el rol
        cargarClientesDesdeBaseDatos();
    }

    /**
     * Actualizar el contador de clientes en la interfaz
     */
    private void actualizarContadorClientes() {
        if (lbl_TotalClientes != null) {
            int totalClientes = tb_Clientes.getItems().size();
            lbl_TotalClientes.setText("Total: " + totalClientes + " clientes");
        }
    }

    /**
     * Configurar elementos de paginaciÃ³n
     */
    private void configurarPaginacion() {
        // Configurar ComboBox de registros por pÃ¡gina
        if (cmb_RegistrosPorPagina != null) {
            cmb_RegistrosPorPagina.getItems().addAll(5, 10, 15, 20, 25, 50);
            cmb_RegistrosPorPagina.setValue(registrosPorPagina);
            cmb_RegistrosPorPagina.setOnAction(e -> cambiarRegistrosPorPagina());
        }

        // Configurar eventos de botones de paginaciÃ³n
        if (btn_PrimeraPagina != null) {
            btn_PrimeraPagina.setOnAction(e -> irAPrimeraPagina());
        }
        if (btn_PaginaAnterior != null) {
            btn_PaginaAnterior.setOnAction(e -> irAPaginaAnterior());
        }
        if (btn_PaginaSiguiente != null) {
            btn_PaginaSiguiente.setOnAction(e -> irAPaginaSiguiente());
        }
        if (btn_UltimaPagina != null) {
            btn_UltimaPagina.setOnAction(e -> irAUltimaPagina());
        }

        // Configurar campo de pÃ¡gina actual
        if (txt_PaginaActual != null) {
            txt_PaginaActual.setOnAction(e -> irAPaginaEspecifica());
        }
    }
}
