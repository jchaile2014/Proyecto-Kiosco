package org.kiosco.igu;

import org.kiosco.logica.Producto;
import org.kiosco.persistencia.ProductoDAO;

import javax.swing.*;
import java.awt.*;

public class FormBuscarProducto extends JFrame {

    private JTextField txtCodigo;
    private JLabel lblResultado;

    public FormBuscarProducto() {
        setTitle("Buscar Producto");
        setSize(350, 250);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));

        panel.add(new JLabel("Código:"));
        txtCodigo = new JTextField();
        panel.add(txtCodigo);

        JButton btnBuscar = new JButton("Buscar");
        panel.add(btnBuscar);

        lblResultado = new JLabel("");
        panel.add(lblResultado);

        add(panel);

        btnBuscar.addActionListener(e -> {
            try {
                buscarProducto();
            } catch (Exception ex) {
                FormUtils.manejarError(this, ex);
            }
        });
    }

    private void buscarProducto() {
        int codigo = FormUtils.parseEntero(txtCodigo, "Código");

        ProductoDAO dao = new ProductoDAO();
        Producto p = dao.buscarPorCodigo(codigo);

        if (p != null) {
            lblResultado.setText("<html>" + p.getNombre() + "<br>$" + p.getPrecio() + " - Stock: " + p.getStock() + "</html>");
        } else {
            lblResultado.setText("No encontrado");
        }
    }
}
