package org.kiosco.igu;

import org.kiosco.logica.Cliente;
import org.kiosco.logica.Fiado;
import org.kiosco.persistencia.ClienteDAO;
import org.kiosco.persistencia.FiadoDAO;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

public class FormFiado extends JFrame {

    private JTextField txtCodigoCliente;
    private JTextField txtArticulo;
    private JTextField txtMonto;
    private JLabel lblTotal;

    public FormFiado() {
        setTitle("Registrar Fiado");
        setSize(350, 280);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(5, 2, 5, 5));

        panel.add(new JLabel("Código cliente:"));
        txtCodigoCliente = new JTextField();
        panel.add(txtCodigoCliente);

        panel.add(new JLabel("Artículo:"));
        txtArticulo = new JTextField();
        panel.add(txtArticulo);

        panel.add(new JLabel("Monto:"));
        txtMonto = new JTextField();
        panel.add(txtMonto);

        JButton btnRegistrar = new JButton("Registrar fiado");
        panel.add(btnRegistrar);

        lblTotal = new JLabel("");
        panel.add(lblTotal);

        add(panel);

        btnRegistrar.addActionListener(e -> {
            try {
                registrarFiado();
            } catch (Exception ex) {
                FormUtils.manejarError(this, ex);
            }
        });
    }

    private void registrarFiado() {
        int codigoCliente = FormUtils.parseEntero(txtCodigoCliente, "Código cliente");
        String articulo = FormUtils.requireTexto(txtArticulo, "Artículo");
        double monto = FormUtils.parsePositivo(txtMonto, "Monto");

        ClienteDAO clienteDAO = new ClienteDAO();
        Cliente cliente = clienteDAO.buscarPorCodigo(codigoCliente);

        if (cliente == null) {
            JOptionPane.showMessageDialog(this, "Cliente no encontrado.");
            return;
        }

        Fiado fiado = new Fiado(cliente, articulo, monto, LocalDate.now());
        FiadoDAO fiadoDAO = new FiadoDAO();
        fiadoDAO.registrar(fiado);

        double total = fiadoDAO.calcularTotalPorCliente(codigoCliente);
        lblTotal.setText("Cuenta total de " + cliente.getNombre() + ": $" + total);
    }
}
