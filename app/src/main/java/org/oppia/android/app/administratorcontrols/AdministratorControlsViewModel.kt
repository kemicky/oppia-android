package org.oppia.android.app.administratorcontrols

import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.ObservableField
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import org.oppia.android.app.administratorcontrols.administratorcontrolsitemviewmodel.AdministratorControlsAccountActionsViewModel
import org.oppia.android.app.administratorcontrols.administratorcontrolsitemviewmodel.AdministratorControlsAppInformationViewModel
import org.oppia.android.app.administratorcontrols.administratorcontrolsitemviewmodel.AdministratorControlsDownloadPermissionsViewModel
import org.oppia.android.app.administratorcontrols.administratorcontrolsitemviewmodel.AdministratorControlsGeneralViewModel
import org.oppia.android.app.administratorcontrols.administratorcontrolsitemviewmodel.AdministratorControlsItemViewModel
import org.oppia.android.app.administratorcontrols.administratorcontrolsitemviewmodel.AdministratorControlsProfileViewModel
import org.oppia.android.app.fragment.FragmentScope
import org.oppia.android.app.model.DeviceSettings
import org.oppia.android.app.model.ProfileId
import org.oppia.android.app.shim.IntentFactoryShim
import org.oppia.android.domain.oppialogger.OppiaLogger
import org.oppia.android.domain.profile.ProfileManagementController
import org.oppia.android.util.data.AsyncResult
import org.oppia.android.util.data.DataProviders.Companion.toLiveData
import javax.inject.Inject

/** [ViewModel] for [AdministratorControlsFragment]. */
@FragmentScope
class AdministratorControlsViewModel @Inject constructor(
  private val activity: AppCompatActivity,
  private val fragment: Fragment,
  private val oppiaLogger: OppiaLogger,
  private val profileManagementController: ProfileManagementController,
  private val IntentFactoryShim: IntentFactoryShim
) {
  private val routeToProfileListListener = activity as RouteToProfileListListener
  private val loadProfileListListener = activity as LoadProfileListListener
  private val showLogoutDialogListener = activity as ShowLogoutDialogListener
  private lateinit var userProfileId: ProfileId
  val selectedFragmentIndex = ObservableField<Int>(1)

  private val deviceSettingsLiveData: LiveData<DeviceSettings> by lazy {
    Transformations.map(
      profileManagementController.getDeviceSettings().toLiveData(),
      ::processGetDeviceSettingsResult
    )
  }

  val administratorControlsLiveData: LiveData<List<AdministratorControlsItemViewModel>> by lazy {
    Transformations.map(deviceSettingsLiveData, ::processAdministratorControlsList)
  }

  private fun processGetDeviceSettingsResult(
    deviceSettingsResult: AsyncResult<DeviceSettings>
  ): DeviceSettings {
    return when (deviceSettingsResult) {
      is AsyncResult.Failure -> {
        oppiaLogger.e(
          "AdministratorControlsFragment", "Failed to retrieve profile", deviceSettingsResult.error
        )
        DeviceSettings.getDefaultInstance()
      }
      is AsyncResult.Pending -> DeviceSettings.getDefaultInstance()
      is AsyncResult.Success -> deviceSettingsResult.value
    }
  }

  private fun processAdministratorControlsList(
    deviceSettings: DeviceSettings
  ): List<AdministratorControlsItemViewModel> {
    val itemViewModelList: MutableList<AdministratorControlsItemViewModel> = mutableListOf(
      AdministratorControlsGeneralViewModel()
    )
    itemViewModelList.add(
      AdministratorControlsProfileViewModel(
        routeToProfileListListener,
        loadProfileListListener
      )
    )
    itemViewModelList.add(
      AdministratorControlsDownloadPermissionsViewModel(
        fragment,
        oppiaLogger,
        profileManagementController,
        userProfileId,
        deviceSettings
      )
    )
    itemViewModelList.add(AdministratorControlsAppInformationViewModel(activity))
    itemViewModelList.add(
      AdministratorControlsAccountActionsViewModel(
        showLogoutDialogListener
      )
    )

    return itemViewModelList
  }

  fun setProfileId(profileId: ProfileId) {
    userProfileId = profileId
  }
}
