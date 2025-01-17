package com.example.imeiscanner.ui.fragments.settings

import android.Manifest.permission.CAMERA
import android.app.Dialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageView
import com.canhub.cropper.options
import com.example.imeiscanner.R
import com.example.imeiscanner.database.*
import com.example.imeiscanner.databinding.FragmentSettingsBinding
import com.example.imeiscanner.ui.fragments.base.BaseFragment
import com.example.imeiscanner.utilits.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SettingsFragment : BaseFragment(R.layout.fragment_settings) {

    private var binding: FragmentSettingsBinding? = null
    private val CAMERA_SELF_PERMISSION = ContextCompat.checkSelfPermission(MAIN_ACTIVITY, CAMERA)
    private val dialog = Dialog(MAIN_ACTIVITY)
    private var coroutineScope: CoroutineScope? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onResume() {
        super.onResume()
        setHasOptionsMenu(true)
        MAIN_ACTIVITY.title = getString(R.string.setttings)
        initFields()
        initClicks()
        when (sharedPreferences.getString(LANG, "")) {
            "en" -> {
                binding!!.settingsLanguage.text = getString(R.string.english)
            }

            "tr" -> {
                binding!!.settingsLanguage.text = "Türkçe"
            }

            "uz" -> {
                binding!!.settingsLanguage.text = "O'zbekcha"
            }
        }
        binding!!.settingsLanguage.setOnClickListener { changeLanguage() }
    }

    override fun onStop() {
        super.onStop()
        MAIN_ACTIVITY.title = getString(R.string.app_name)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) changePhoto()
        }

    private fun initClicks() {
        binding!!.settingsChangePhoto.setOnClickListener {
            if (CAMERA_SELF_PERMISSION == PackageManager.PERMISSION_GRANTED) changePhoto()
            else requestPermissionLauncher.launch(CAMERA)
        }
        binding!!.settingsUserNameChange.setOnClickListener { replaceFragment(ChangeUserNameFragment()) }
    }

    private fun initFields() {
        dialog.setContentView(R.layout.item_progress_deletiing)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(com.google.zxing.client.android.R.color.zxing_transparent)
        dialog.setCancelable(false)

        coroutineScope = CoroutineScope(Dispatchers.IO)

        binding!!.settingsUserName.text = USER.fullname
//        binding!!.settingsPhoneChange.text = USER.email
        binding!!.settingsUserPhoto.photoDownloadAndSet(USER.photoUrl)
        if (userGoogleOrPhone() == GOOGLE_PROVIDER_ID) {
            binding!!.settingsEmailOrPhoneText.text = USER.email
            binding!!.settingsEmailText.text = getString(R.string.email)
        } else {
            binding!!.settingsEmailText.text = getString(R.string.register_text_phoneNumber)
            binding!!.settingsEmailOrPhoneText.text = USER.phone
        }
        binding!!.settingsUserNameChange.setOnClickListener { replaceFragment(ChangeUserNameFragment()) }
        binding!!.settingsLogOutBtn.setOnClickListener { logOutDialog() }
    }

    private fun changePhoto() {
        cropImage.launch(
            options {
                setGuidelines(CropImageView.Guidelines.ON)
                setCropShape(CropImageView.CropShape.OVAL)
                setRequestedSize(600, 600)
                setAspectRatio(1, 1)
            }
        )
    }

    private val cropImage = registerForActivityResult(CropImageContract()) { result ->
        val uri = result.originalUri
        val path = REF_STORAGE_ROOT.child(FOLDER_PROFILE_IMAGE).child(CURRENT_UID)

        if (uri != null) {
            putFileToStorage(path, uri) {
                getUrlFromStorage(path) { task ->
                    putUserPhotoUrlToDatabase(task) {
                        binding!!.settingsUserPhoto.photoDownloadAndSet(task)
                        MAIN_ACTIVITY.mAppDrawer.updateHeader()
                        USER.photoUrl = task
                        updateUserPhotoUrl(task)
                        MAIN_ACTIVITY.mAppDrawer.updateHeader()
                        showToast(getString(R.string.image_changed))
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        MAIN_ACTIVITY.menuInflater.inflate(R.menu.settings_action_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings_delete_user -> accountDeleteDialog(onPositiveButton = {
                dialog.show()
                coroutineScope?.launch {
                    delay(250)
                    deleteAlLImagesFromStorage(onSuccess = {
                        deleteUserDatasFromDatabase()
                        deleteUser()
                        dialog.dismiss()
                    })
                }
            })
        }
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        dialog.dismiss()
        coroutineScope = null

    }

}