package ir.arandy.naghshan.ui.invoice;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import ir.arandy.naghshan.R;
import ir.arandy.naghshan.databinding.FragmentInvoiceBinding;
import ir.arandy.naghshan.model.Invoice;
import ir.arandy.naghshan.model.Kala;
import ir.arandy.naghshan.ui.output.OutputFragment;
import ir.arandy.naghshan.util.MoneyFormatter;
import ir.arandy.naghshan.util.PersianNumberParser;

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
        adapter.setOnItemClickListener((k, pos) -> showEditDialog(k, pos));

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

    /**
     * نمایش دیالوگ ویرایش قیمت جنس و دستمزد یک قلم.
     * مبلغ کل ردیف به‌صورت خودکار بازمحاسبه می‌شود.
     */
    private void showEditDialog(Kala k, int position) {
        Log.i("NAGHSHAN_INVOICE",
                "showEditDialog: row=" + k.getRowNumber() + " name=" + k.getProductName());

        View v = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_edit_price, null, false);
        EditText eMat = v.findViewById(R.id.editMaterial);
        EditText eLab = v.findViewById(R.id.editLabor);

        eMat.setText(String.valueOf(k.getMaterialPrice()));
        eLab.setText(String.valueOf(k.getLaborPrice()));

        new AlertDialog.Builder(requireContext())
                .setTitle("ویرایش: " + (k.getProductName() == null ? "" : k.getProductName()))
                .setView(v)
                .setPositiveButton("ذخیره", (d, w) -> {
                    long mat = PersianNumberParser.parseLong(eMat.getText().toString());
                    long lab = PersianNumberParser.parseLong(eLab.getText().toString());

                    Log.i("NAGHSHAN_INVOICE",
                            "edit saved: mat=" + mat + " lab=" + lab
                                    + " qty=" + k.getQuantity());

                    k.setMaterialPrice(mat);
                    k.setLaborPrice(lab);
                    k.setTotalPrice(Math.round(k.getQuantity() * (mat + lab)));

                    adapter.notifyItemChanged(position);

                    // به‌روزرسانی جمع کل در پایین صفحه
                    Invoice cur = vm.getCurrent();
                    if (cur != null) {
                        b.txtTotal.setText(MoneyFormatter.formatWithRial(cur.getGrandTotal()));
                    }
                })
                .setNegativeButton("انصراف", null)
                .show();
    }

    private static String safe(String s) { return s == null || s.isEmpty() ? "-" : s; }
}