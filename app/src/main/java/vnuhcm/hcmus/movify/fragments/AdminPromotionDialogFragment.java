package vnuhcm.hcmus.movify.fragments;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.models.Promotion;

public class AdminPromotionDialogFragment extends DialogFragment {

    public interface Listener {
        void onSavePromotion(Promotion promotion);
    }

    private Listener listener;
    private Promotion editing;

    public static AdminPromotionDialogFragment newInstance(@Nullable Promotion promo) {
        AdminPromotionDialogFragment f = new AdminPromotionDialogFragment();
        f.editing = promo;
        return f;
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (listener != null) {
            return;
        }

        if (getParentFragment() instanceof Listener) {
            listener = (Listener) getParentFragment();
        } else if (context instanceof Listener) {
            listener = (Listener) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_admin_promotion, container, false);

        EditText etCode = view.findViewById(R.id.etPromoCode);
        Spinner spType = view.findViewById(R.id.spinnerPromoType);
        EditText etValue = view.findViewById(R.id.etPromoValue);
        TextView btnCancel = view.findViewById(R.id.btnCancelPromo);
        TextView btnSave = view.findViewById(R.id.btnSavePromo);

        String[] typeLabels = new String[]{
                getString(R.string.admin_promotion_type_percent),
                getString(R.string.admin_promotion_type_vnd)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, typeLabels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spType.setAdapter(adapter);

        if (editing != null) {
            etCode.setText(editing.getCode());
            spType.setSelection("percent".equalsIgnoreCase(editing.getType()) ? 0 : 1);
            etValue.setText(String.valueOf(editing.getDiscountValue()));
        }

        btnCancel.setOnClickListener(v -> dismiss());

        btnSave.setOnClickListener(v -> {
            String code = etCode.getText().toString().trim();
            String type = spType.getSelectedItemPosition() == 0 ? "percent" : "vnd";
            String valueStr = etValue.getText().toString().trim();

            if (TextUtils.isEmpty(code) || TextUtils.isEmpty(valueStr)) {
                Toast.makeText(requireContext(), R.string.fill_required_fields, Toast.LENGTH_SHORT).show();
                return;
            }

            double val;
            try {
                val = Double.parseDouble(valueStr);
            } catch (NumberFormatException ex) {
                Toast.makeText(requireContext(), R.string.admin_promotion_value_invalid, Toast.LENGTH_SHORT).show();
                return;
            }

            Promotion p = editing != null ? editing : new Promotion();
            p.setCode(code);
            p.setType(type);
            p.setDiscountValue(val);

            if (listener != null) listener.onSavePromotion(p);
            dismiss();
        });

        return view;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }
}
