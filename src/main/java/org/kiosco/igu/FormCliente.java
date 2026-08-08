package org.kiosco.igu;

import org.kiosco.logica.Cliente;
import org.kiosco.persistencia.ClienteDAO;

import javax.swing.*;
import java.awt.*;

public class FormCliente extends JFrame {

    private JTextField txtNombre;
    private JTextField txtApellido;
    private JTextField txtTelefono;

    public FormCliente() {
        setTitle("Agregar Cliente");
        setSize(350, 220);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(4, 2, 5, 5));

        panel.add(new JLabel("Nombre:"));
        txtNombre = new JTextField();
        panel.add(txtNombre);

        panel.add(new JLabel("Apellido:"));
        txtApellido = new JTextField();
        panel.add(txtApellido);

        panel.add(new JLabel("Teléfono:"));
        txtTelefono = new JTextField();
        panel.add(txtTelefono);

        JButton btnGuardar = new JButton("Guardar");
        panel.add(btnGuardar);

        add(panel);

        btnGuardar.addActionListener(e -> {
            try {
                guardarCliente();
            } catch (Exception ex) {
                FormUtils.manejarError(this, ex);
            }
        });
    }

    private void guardarCliente() {
        String nombre = FormUtils.requireTexto(txtNombre, "Nombre");
        String apellido = FormUtils.requireTexto(txtApellido, "Apellido");
        String telefono = FormUtils.requireTexto(txtTelefono, "Teléfono");

        Cliente c = new Cliente(nombre, apellido, telefono);

        ClienteDAO dao = new ClienteDAO();
        dao.guardar(c);

        JOptionPane.showMessageDialog(this, "Cliente guardado con código: " + c.getCodigo());
        dispose();
    }
}
