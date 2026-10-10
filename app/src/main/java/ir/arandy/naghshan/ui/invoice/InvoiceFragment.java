package ir.arandy.naghshan.ui.invoice;

import android.os.Bundle;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

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

        Invoice inv = (Invoice) getArguments().getSerializable(ARG_INVOICE);
        if (inv != null) vm.setInvoice(inv);

        vm.getInvoice().observe(getViewLifecycleOwner(), i -> {
            if (i == null) return;
            adapter.submit(i);
            b.txtProjectNumber.setText(i.getProjectInfo().getProjectNumber());
            b.txtApplicant.setText(i.getProjectInfo().getApplicantName());
            b.txtRequestNumber.setText(i.getProjectInfo().getRequestNumber());
            b.txtItemsCount.setText("لیست کالاها (" + i.getItems().size() + " قلم)");
            b.txtTotal.setText(MoneyFormatter.formatWithRial(i.getGrandTotal()));
        });

        b.btnApplyChanges.setOnClickListener(x -> openOutput());
        b.btnExport.setOnClickListener(x -> openOutput());
    }

    private void openOutput() {
        Invoice inv = vm.getCurrent();
        if (inv == null) return;
        Bundle args = new Bundle();
        args.putSerializable(OutputFragment.ARG_INVOICE, inv);
        androidx.navigation.Navigation.findNavController(requireView())
                .navigate(ir.arandy.naghshan.R.id.action_invoice_to_output, args);
    }
}