package vnuhcm.hcmus.movify.activities.profile;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.services.AuthApiService;
import vnuhcm.hcmus.movify.services.UploadService;
import vnuhcm.hcmus.movify.utils.RemoteImageLoader;

public class EditProfileActivity extends AppCompatActivity {

	public static final String EXTRA_FULL_NAME = "extra_full_name";
	public static final String EXTRA_PHONE_NUMBER = "extra_phone_number";
	public static final String EXTRA_GENDER = "extra_gender";
	public static final String EXTRA_DATE_OF_BIRTH = "extra_date_of_birth";
	public static final String EXTRA_AVATAR = "extra_avatar";

	private ImageView ivBack;
	private ImageView ivAvatar;
	private EditText etFullName;
	private EditText etPhoneNumber;
	private Spinner spinnerGender;
	private TextView tvDateOfBirth;
	private TextView btnPickDate;
	private TextView btnChangeAvatar;
	private TextView btnSave;
	private TextView btnCancel;

	private AuthApiService authApiService;
	private UploadService uploadService;

	private String selectedDateOfBirth;
	private String selectedAvatar;
	private Uri selectedImageUri;
	private final ActivityResultLauncher<PickVisualMediaRequest> pickAvatarLauncher =
			registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
				if (uri != null) {
					selectedImageUri = uri;
					selectedAvatar = null;
					ivAvatar.setImageURI(uri);
				}
			});

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit_profile);
		authApiService = new AuthApiService(this);
		uploadService = new UploadService(this);

		initViews();
		setupGenderSpinner();
		bindFromIntent();
		setupActions();
	}

	private void initViews() {
		ivBack = findViewById(R.id.ivBack);
		ivAvatar = findViewById(R.id.ivAvatar);
		etFullName = findViewById(R.id.etFullName);
		etPhoneNumber = findViewById(R.id.etPhoneNumber);
		spinnerGender = findViewById(R.id.spinnerGender);
		tvDateOfBirth = findViewById(R.id.tvDateOfBirth);
		btnPickDate = findViewById(R.id.btnPickDate);
		btnChangeAvatar = findViewById(R.id.btnChangeAvatar);
		btnSave = findViewById(R.id.btnSave);
		btnCancel = findViewById(R.id.btnCancel);
	}

	private void setupGenderSpinner() {
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this,
				R.array.profile_gender_options,
				android.R.layout.simple_spinner_item
		);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerGender.setAdapter(adapter);
	}

	private void bindFromIntent() {
		Intent intent = getIntent();

		String fullName = intent.getStringExtra(EXTRA_FULL_NAME);
		String phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER);
		String gender = intent.getStringExtra(EXTRA_GENDER);
		selectedDateOfBirth = intent.getStringExtra(EXTRA_DATE_OF_BIRTH);
		selectedAvatar = intent.getStringExtra(EXTRA_AVATAR);
		selectedImageUri = null;

		etFullName.setText(fullName);
		etPhoneNumber.setText(phoneNumber);
		tvDateOfBirth.setText(formatOrPlaceholder(selectedDateOfBirth, getString(R.string.profile_select_date_hint)));

		if (!TextUtils.isEmpty(gender)) {
			selectGender(gender);
		}

		updateAvatarPreview();
	}

	private void setupActions() {
		ivBack.setOnClickListener(v -> finish());
		btnCancel.setOnClickListener(v -> finish());

		btnPickDate.setOnClickListener(v -> openDatePicker());
		btnChangeAvatar.setOnClickListener(v -> {
			pickAvatarLauncher.launch(new PickVisualMediaRequest.Builder()
					.setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
					.build());
		});

		btnSave.setOnClickListener(v -> saveProfile());
	}

	private void openDatePicker() {
		Calendar calendar = Calendar.getInstance();

		DatePickerDialog dialog = new DatePickerDialog(
				this,
				(view, year, month, dayOfMonth) -> {
					selectedDateOfBirth = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
					tvDateOfBirth.setText(selectedDateOfBirth);
				},
				calendar.get(Calendar.YEAR),
				calendar.get(Calendar.MONTH),
				calendar.get(Calendar.DAY_OF_MONTH)
		);

		dialog.show();
	}

	private void saveProfile() {
		final String fullName = getFullNameInput();
		final String phoneNumber = getPhoneNumberInput();
		final String gender = getSelectedGender();
		final String dateOfBirth = selectedDateOfBirth;
		final String currentAvatarUrl = selectedAvatar;

		btnSave.setEnabled(false);

		if (selectedImageUri != null) {
			uploadService.uploadImage(selectedImageUri, authApiService.getJwtToken(), new UploadService.UploadCallback() {
				@Override
				public void onSuccess(String imageUrl) {
					selectedAvatar = imageUrl;
					updateProfileAfterUpload(fullName, phoneNumber, imageUrl, gender, dateOfBirth);
				}

				@Override
				public void onFailure(String errorMessage) {
					runOnUiThread(() -> {
						btnSave.setEnabled(true);
						Toast.makeText(EditProfileActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
					});
				}
			});
			return;
		}

		updateProfileAfterUpload(fullName, phoneNumber, currentAvatarUrl, gender, dateOfBirth);
	}

	private void updateProfileAfterUpload(String fullName,
	                                     String phoneNumber,
	                                     String avatar,
	                                     String gender,
	                                     String dateOfBirth) {
		new Thread(() -> {
			try {
				authApiService.updateProfile(fullName, phoneNumber, avatar, gender, dateOfBirth);

				runOnUiThread(() -> {
					Intent result = new Intent();
					result.putExtra(EXTRA_FULL_NAME, fullName);
					result.putExtra(EXTRA_PHONE_NUMBER, phoneNumber);
					result.putExtra(EXTRA_GENDER, gender);
					result.putExtra(EXTRA_DATE_OF_BIRTH, dateOfBirth);
					result.putExtra(EXTRA_AVATAR, avatar);
					setResult(RESULT_OK, result);
					finish();
				});
			} catch (Exception exception) {
				runOnUiThread(() -> {
					btnSave.setEnabled(true);
					String message = exception.getMessage();
					if (TextUtils.isEmpty(message)) {
						message = getString(R.string.profile_load_error);
					}
					Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
				});
			}
		}).start();
	}

	private String getFullNameInput() {
		return textOrNull(etFullName.getText().toString().trim());
	}

	private String getPhoneNumberInput() {
		return textOrNull(etPhoneNumber.getText().toString().trim());
	}

	private String getSelectedGender() {
		Object selectedItem = spinnerGender.getSelectedItem();
		return selectedItem == null ? null : selectedItem.toString();
	}

	private void selectGender(String gender) {
		ArrayAdapter<?> adapter = (ArrayAdapter<?>) spinnerGender.getAdapter();
		if (adapter == null) {
			return;
		}

		for (int i = 0; i < adapter.getCount(); i++) {
			Object item = adapter.getItem(i);
			if (item != null && item.toString().equalsIgnoreCase(gender)) {
				spinnerGender.setSelection(i);
				return;
			}
		}
	}

	private void updateAvatarPreview() {
		if (selectedImageUri != null) {
			ivAvatar.setImageURI(selectedImageUri);
			return;
		}

		RemoteImageLoader.loadAvatar(ivAvatar, selectedAvatar);

		if (TextUtils.isEmpty(selectedAvatar)) {
			ivAvatar.setImageResource(R.drawable.ic_user);
		}
	}

	private String formatOrPlaceholder(String value, String placeholder) {
		return TextUtils.isEmpty(value) ? placeholder : value;
	}

	private String textOrNull(String value) {
		return TextUtils.isEmpty(value) ? null : value;
	}
}
