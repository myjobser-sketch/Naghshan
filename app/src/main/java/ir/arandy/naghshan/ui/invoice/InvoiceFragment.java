package ir.arandy.naghshan.ui.invoice;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import ir.arandy.naghshan.R;
import ir.arandy.naghshan.databinding.FragmentInvoiceBinding;
import ir.arandy.naghshan.model.Invoice;
import ir.arandy.naghshan.ui.output.OutputFragment;
import ir.arandy.naghshan.util.MoneyFormatter;

public class InvoiceFragment extends Fragment {

    public static final String ARG_INVOICE = "invoice";

    private FragmentInvoiceBinding b;
    private InvoiceViewModel vm;
    private InvoiceAdapter adapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, ViewGroup c, Bundle s) {
        b = FragmentInvoiceBinding.inflate(inf, c, false);
        return b.getRoot();
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);

        vm = new ViewModelProvider(requireActivity()).get(InvoiceViewModel.class);

        adapter = new InvoiceAdapter();
        b.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.recycler.setAdapter(adapter);

        // بازگشت با کلیک روی فلش Toolbar
        b.toolbar.setNavigationOnClickListener(x -> requireActivity().onBackPressed());

        if (getArguments() != null) {
            Invoice inv = (Invoice) getArguments().getSerializable(ARG_INVOICE);
            if (inv != null) vm.setInvoice(inv);
        }

        vm.getInvoice().observe(getViewLifecycleOwner(), i -> {
            if (i == null) return;
            adapter.submit(i);
            b.txtProjectNumber.setText(safe(i.getProjectInfo().getProjectNumber()));
            b.txtApplicant.setText(safe(i.getProjectInfo().getApplicantName()));
            b.txtRequestNumber.setText(safe(i.getProjectInfo().getRequestNumber()));
            b.txtItemsCount.setText("لیست کالاها (" + i.getItemsCount() + " قلم)");
            b.txtTotal.setText(MoneyFormatter.formatWithRial(i.getGrandTotal()));
        });

        b.btnApplyChanges.setOnClickListener(x -> openOutput());
        b.btnExport.setOnClickListener(x -> openOutput());
    }

    /**
     * حرکت به OutputFragment با استفاده از FragmentManager.
     * (به‌جای NavController که در این Activity وجود ندارد.)
     */
    private void openOutput() {
        Invoice inv = vm.getCurrent();
        if (inv == null) return;

        OutputFragment out = new OutputFragment();
        Bundle args = new Bundle();
        args.putSerializable(OutputFragment.ARG_INVOICE, inv);
        out.setArguments(args);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, out)
                .addToBackStack(null)
                .commit();
    }

    private static String safe(String s) { return s == null || s.isEmpty() ? "-" : s; }
}