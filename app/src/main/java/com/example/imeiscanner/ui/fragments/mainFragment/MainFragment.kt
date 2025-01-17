package com.example.imeiscanner.ui.fragments.mainFragment


import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.imeiscanner.R
import com.example.imeiscanner.database.CHILD_IMEI1
import com.example.imeiscanner.database.CHILD_PHONE_PHOTOS
import com.example.imeiscanner.database.CURRENT_UID
import com.example.imeiscanner.database.NODE_PHONE_DATA_INFO
import com.example.imeiscanner.database.REF_DATABASE_ROOT
import com.example.imeiscanner.database.REF_STORAGE_ROOT
import com.example.imeiscanner.database.getUrlFromStorage
import com.example.imeiscanner.databinding.FragmentMainBinding
import com.example.imeiscanner.models.PhoneDataModel
import com.example.imeiscanner.ui.fragments.SearchFragment
import com.example.imeiscanner.ui.fragments.add_phone.PhoneAddFragment
import com.example.imeiscanner.ui.fragments.add_phone.PhoneInfoFragment
import com.example.imeiscanner.utilits.*
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.SnapshotParser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.GenericTypeIndicator
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlin.math.log

class MainFragment() : Fragment() {

    private var oldestBtn: MenuItem? = null
    private var newestBtn: MenuItem? = null
    private var searchItem: MenuItem? = null
    private var searchWithQRCode: MenuItem? = null
    private var binding: FragmentMainBinding? = null            //
    private var rv: RecyclerView? = null
    private var refPhoneData: DatabaseReference? = null
    private var adapter: FirebaseRecyclerAdapter<PhoneDataModel, MainAdapter.PhonesHolder>? = null
    private var scannerButton: ImageView? = null
    private var scanOptions: ScanOptions? = null                  ///
    private var options: FirebaseRecyclerOptions<PhoneDataModel>? = null          /////
    private var searchView: SearchView? = null
    private var linerLayoutManager: LinearLayoutManager? = null          ////
    private var popupMenu: PopupMenu? = null

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents == null) {
            showToast(getString(R.string.cancelled_from_barcode))
        } else {
            installResultForET(result.contents)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onResume() {
        super.onResume()

        setHasOptionsMenu(true)
        MAIN_ACTIVITY.title = getString(R.string.app_name)
        MAIN_ACTIVITY.supportFragmentManager.popBackStack()// orqaga qaytishni o'chiradi
        MAIN_ACTIVITY.mAppDrawer.enableDrawer()
        initSort()
        initFields()
        hideKeyboard()
        initRecyclerView()
        checkDataExists()
        listenerToolbar()
        binding!!.floatActionBtn.setOnClickListener {
            replaceFragment(PhoneAddFragment())
        }
        initPopupMenu()
    }

    private fun initPopupMenu() {
        popupMenu = PopupMenu(MAIN_ACTIVITY, binding?.toolbarItemMenu)
        popupMenu?.menuInflater?.inflate(R.menu.select_menu, popupMenu?.menu)
        popupMenu?.setOnMenuItemClickListener {
            val id = it.itemId
            if (id == R.id.select_all) {
                (adapter as MainAdapter).selectAll()
            } else if (id == R.id.unselect_all) {
                (adapter as MainAdapter).unselectAll()
            }
            false
        }
        binding!!.toolbarItemMenu.setOnClickListener { popupMenu?.show() }
    }

    private fun listenerToolbar() {
        binding!!.toolbarItemLcStar.setOnClickListener { addFavourite() }
        binding!!.toolbarItemLcDelete.setOnClickListener { delete() }
        binding!!.toolbarItemLcCancel.setOnClickListener { cancel() }
    }

    private fun addFavourite() {
        cancelBinding()
        (adapter as MainAdapter).addFavouritesSelectedI()
    }

    private fun cancelBinding() {
        binding!!.toolbarItem.visibility = View.GONE
        MAIN_ACTIVITY.mToolbar.visibility = View.VISIBLE
    }

    private fun cancel() {
        cancelBinding()
        (adapter as MainAdapter).cancelItemSelecting()
    }

    private fun delete() {
        cancelBinding()
        (adapter as MainAdapter).deleteSelectedItem()
    }

    private fun initSort() {
        linerLayoutManager = LinearLayoutManager(MAIN_ACTIVITY)
        if (sharedPreferences.getBoolean(STATE, false)) {
            //engyangi qo'shilganini birinchi ko'rsatadi
            linerLayoutManager!!.reverseLayout = true
            linerLayoutManager!!.stackFromEnd = true
        } else {
            //eng ynagi qo'shilganlarini en pasida ko'rsatadi
            linerLayoutManager!!.reverseLayout = false
            linerLayoutManager!!.stackFromEnd = false
        }
    }

    private fun initFields() {
        rv = binding?.rvMainFragment
        rv?.layoutManager = linerLayoutManager
        rv?.setHasFixedSize(true)
        scanOptions = ScanOptions()
        scanOptions(scanOptions!!)
    }

    private fun installResultForET(result: String) {
        searchView?.setQuery(result, false)
        searchInit()
    }

    override fun onPause() {
        super.onPause()
        adapter?.stopListening()
    }

    private fun initRecyclerView() {
        refPhoneData = REF_DATABASE_ROOT.child(NODE_PHONE_DATA_INFO).child(CURRENT_UID)

        val options = FirebaseRecyclerOptions.Builder<PhoneDataModel>()
            .setQuery(refPhoneData!!, SnapshotParser { snapshot ->
                val phoneData = snapshot.getValue(PhoneDataModel::class.java)
                if (snapshot.child(CHILD_PHONE_PHOTOS).value as? List<*> != null) {
                    phoneData?.photoList = snapshot.child(CHILD_PHONE_PHOTOS).value as List<String>
                }
//                phoneData?.photoList = snapshot.child(CHILD_PHONE_PHOTOS)
//                    .getValue(object : GenericTypeIndicator<List<String>>() {})
                phoneData!!
            }).build()

        adapter = MainAdapter(options) { show -> showItemToolbar(show) }
        adapter!!.itemCount.toString()
        rv?.adapter = adapter
        adapter?.startListening()
        clickItem()
        (adapter as MainAdapter).initCountView(binding!!.toolbarItemLcCount)
        (adapter as MainAdapter).initFloatButton(binding!!.floatActionBtn)
    }


    private fun checkDataExists() {
        val ref = REF_DATABASE_ROOT.child(NODE_PHONE_DATA_INFO).child(CURRENT_UID)
        ref.addValueEventListener(AppValueEventListener {
            if (it.childrenCount > 0) {
                binding!!.mainEx.visibility = View.GONE
                rv?.visibility = View.VISIBLE
            } else {
                rv?.visibility = View.GONE
                binding!!.mainEx.visibility = View.VISIBLE
            }
        })
    }

    private fun clickItem() {
        (adapter as MainAdapter).itemOnClickListener { item ->
            val bundle = Bundle()
            bundle.putSerializable(POSITION_ITEM, item)
            parentFragmentManager.setFragmentResult(DATA_FROM_MAIN_FRAGMENT, bundle)
            replaceFragment(PhoneInfoFragment())
        }
    }

    private fun mySearch(text: String) {
        options = FirebaseRecyclerOptions.Builder<PhoneDataModel>()
            .setQuery(
                refPhoneData?.orderByChild(CHILD_IMEI1)!!.startAt(text).endAt(text + "\uf8ff"),
                PhoneDataModel::class.java
            ).build()
        adapter = MainAdapter(options!!) {}
        adapter?.startListening()
        rv?.adapter = adapter
        clickItem()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear() // replace fragment bo'lganda eski menuni o'chiradi

        inflater.inflate(R.menu.search_menu, menu)
        newestBtn = menu.findItem(R.id.menu_first_newest)
        oldestBtn = menu.findItem(R.id.menu_first_oldest)
        searchItem = menu.findItem(R.id.menu_search_btn)
        searchWithQRCode = menu.findItem(R.id.menu_scanner_btn)
        searchWithQRCode?.isVisible = true
        scannerButton = searchWithQRCode?.actionView as ImageView
        scannerButton?.setImageResource(R.drawable.ic_qr_code_scanner)
        searchView = searchItem?.actionView as SearchView
        searchInit()
        scannerButton?.setOnClickListener {
            barcodeLauncher.launch(scanOptions)

        }
    }

    private fun searchInit() {
        searchView?.setOnQueryTextListener(AppSearchView {
            mySearch(it)
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_first_newest -> {
                editor.putBoolean(STATE, true)
                editor.apply()
                newestBtn?.isChecked = true
                restartActivity()
            }

            R.id.menu_first_oldest -> {
                editor.putBoolean(STATE, false)
                editor.apply()
                oldestBtn?.isChecked = true
                restartActivity()
            }
        }
        rv?.smoothScrollToPosition(1)     //rv ni eng birinchi positioniga olib chiqadi
        return true
    }

    private fun showItemToolbar(show: Boolean) {
        binding!!.toolbarItem.isVisible = show
    }

    override fun onStop() {
        super.onStop()
        cancel()
    }

    override fun onDestroyView() {
        super.onDestroyView()

//        binding = null
//        oldestBtn = null
        newestBtn = null
        searchWithQRCode = null
//        rv=null
        searchItem = null
        refPhoneData = null
        scannerButton = null
        searchView = null
        popupMenu = null
        adapter = null
        scanOptions = null
        options = null
        linerLayoutManager = null

    }
}