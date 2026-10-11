package ir.arandy.naghshan.ui.output;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import ir.arandy.naghshan.data.repository.InvoiceRepository;
import ir.arandy.naghshan.databinding.FragmentOutputBinding;
import ir.arandy.naghshan.model.Invoice;
import ir.arandy.naghshan.util.MoneyFormatter;

public class OutputFragment extends Fragment {

    public static final String ARG_INVOICE = "invoice";
    private FragmentOutputBinding b;
    private OutputViewModel vm;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, ViewGroup c, Bundle s) {
        b = FragmentOutputBinding.inflate(inf, c, false);
        return b.getRoot();
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);

        vm = new ViewModelProvider(this).get(OutputViewModel.class);

        b.toolbar.setNavigationOnClickListener(x -> requireActivity().onBackPressed());

        if (getArguments() != null) {
            Invoice inv = (Invoice) getArguments().getSerializable(ARG_INVOICE);
            if (inv != null) {
                vm.setInvoice(inv);
                b.txtBaseFileName.setText(
                        "nagh-" + inv.getProjectInfo().getProjectNumber() + ".xlsx");
            }
        }

        b.radioIncrease.setChecked(true);

        b.btnApply.setOnClickListener(x -> apply());
        b.btnExportPdf.setOnClickListener(x -> vm.exportPdf());
        b.btnExportExcel.setOnClickListener(x -> vm.exportExcel());

        vm.getMessage().observe(getViewLifecycleOwner(), m ->
                Toast.makeText(requireContext(), m, Toast.LENGTH_LONG).show());

        vm.getInvoice().observe(getViewLifecycleOwner(), i -> {
            if (i != null) b.txtNewTotal.setText(MoneyFormatter.formatWithRial(i.getGrandTotal()));
        });
    }

    private void apply() {
        double pct = 0;
        try {
            pct = Double.parseDouble(b.editPercent.getText().toString());
        } catch (Exception ignored) {}

        if (!b.radioIncrease.isChecked()) pct = -pct;

        boolean mat = b.checkMaterial.isChecked();
        boolean lab = b.checkLabor.isChecked();

        Invoice base = vm.getInvoice().getValue();
        if (base == null) return;

        Invoice updated = new InvoiceRepository(
                requireContext().getApplicationContext())
                .applyAdjustment(base, pct, mat, lab);
        vm.setInvoice(updated);
    }
}