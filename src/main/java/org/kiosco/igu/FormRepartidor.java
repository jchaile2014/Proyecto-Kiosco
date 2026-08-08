package org.kiosco.igu;

import org.kiosco.logica.Repartidor;
import org.kiosco.persistencia.RepartidorDAO;

import javax.swing.*;
import java.awt.*;

public class FormRepartidor extends JFrame {

    private JTextField txtNombre;
    private JTextField txtTelefono;

    public FormRepartidor() {
        setTitle("Agregar Repartidor");
        setSize(350, 180);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));

        panel.add(new JLabel("Nombre:"));
        txtNombre = new JTextField();
        panel.add(txtNombre);

        panel.add(new JLabel("Teléfono:"));
        txtTelefono = new JTextField();
        panel.add(txtTelefono);

        JButton btnGuardar = new JButton("Guardar");
        panel.add(btnGuardar);

        add(panel);

        btnGuardar.addActionListener(e -> {
            try {
                guardarRepartidor();
            } catch (Exception ex) {
                FormUtils.manejarError(this, ex);
            }
        });
    }

    private void guardarRepartidor() {
        String nombre = FormUtils.requireTexto(txtNombre, "Nombre");
        String telefono = FormUtils.requireTexto(txtTelefono, "Teléfono");

        Repartidor r = new Repartidor(nombre, telefono);

        RepartidorDAO dao = new RepartidorDAO();
        dao.guardar(r);

        JOptionPane.showMessageDialog(this, "Repartidor guardado con código: " + r.getCodigo());
        dispose();
    }
}
