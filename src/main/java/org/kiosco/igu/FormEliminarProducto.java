package org.kiosco.igu;

import org.kiosco.persistencia.ProductoDAO;

import javax.swing.*;
import java.awt.*;

public class FormEliminarProducto extends JFrame {

    private JTextField txtCodigo;

    public FormEliminarProducto() {
        setTitle("Dar de baja Producto");
        setSize(300, 150);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));

        panel.add(new JLabel("Código:"));
        txtCodigo = new JTextField();
        panel.add(txtCodigo);

        JButton btnEliminar = new JButton("Dar de baja");
        panel.add(btnEliminar);

        add(panel);

        btnEliminar.addActionListener(e -> {
            try {
                eliminarProducto();
            } catch (Exception ex) {
                FormUtils.manejarError(this, ex);
            }
        });
    }

    private void eliminarProducto() {
        int codigo = FormUtils.parseEntero(txtCodigo, "Código");

        ProductoDAO dao = new ProductoDAO();
        dao.eliminar(codigo);

        JOptionPane.showMessageDialog(this, "Producto dado de baja.");
        dispose();
    }
}
