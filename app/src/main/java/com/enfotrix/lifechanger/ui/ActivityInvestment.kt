package com.enfotrix.lifechanger.ui

import android.app.Activity
import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.enfotrix.lifechanger.Adapters.InvestmentViewPagerAdapter
import com.enfotrix.lifechanger.Adapters.WithdrawViewPagerAdapter
import com.enfotrix.lifechanger.Constants
import com.enfotrix.lifechanger.Models.InvestmentViewModel
import com.enfotrix.lifechanger.Models.TransactionModel
import com.enfotrix.lifechanger.R
import com.enfotrix.lifechanger.SharedPrefManager
import com.enfotrix.lifechanger.Utils
import com.enfotrix.lifechanger.databinding.ActivityInvestmentBinding
import com.enfotrix.lifechanger.databinding.ActivityWithdrawBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayoutMediator
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.text.BaseColor
import com.itextpdf.text.Document
import com.itextpdf.text.Element
import com.itextpdf.text.FontFactory
import com.itextpdf.text.Paragraph
import com.itextpdf.text.Phrase
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ActivityInvestment : AppCompatActivity() {

    private lateinit var binding: ActivityInvestmentBinding
    private val investmentViewModel: InvestmentViewModel by viewModels()
    private lateinit var utils: Utils
    private lateinit var mContext: Context
    private lateinit var constants: Constants
    private lateinit var sharedPrefManager: SharedPrefManager

    private lateinit var investmentPagerAdapter: InvestmentViewPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInvestmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mContext = this@ActivityInvestment
        utils = Utils(mContext)
        constants = Constants()
        sharedPrefManager = SharedPrefManager(mContext)
        setTitle("My Investement")

        getData()

        withdrawdate()

        investmentPagerAdapter = InvestmentViewPagerAdapter(this, 2)
        binding.viewPager.adapter = investmentPagerAdapter
        binding.imgBack.setOnClickListener {
            finish()
        }

binding.DateFrom.setOnClickListener {
    showDatePickerDialog("From")
}
        binding.DateTo.setOnClickListener {
            showDatePickerDialog("To")
        }
        binding.totalInvestment.text = sharedPrefManager.getInvestment().investmentBalance
    }


    private fun setupTabLayout() {
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            if (position == 0) {
                tab.text = "Approved"
            } else if (position == 1) {
                tab.text = "Pending"
            }
        }.attach()
    }

    private fun setupViewPager() {
        val adapter = InvestmentViewPagerAdapter(this, 2)
        binding.viewPager.adapter = adapter
    }

    private fun getData() {
        utils.startLoadingAnimation()
        lifecycleScope.launch {
            investmentViewModel.getInvestmentReq(sharedPrefManager.getToken())
                .addOnCompleteListener { task ->
                    utils.endLoadingAnimation()
                    if (task.isSuccessful) {
                        val list = ArrayList<TransactionModel>()
                        if (task.result.size() > 0) {
                            for (document in task.result) list.add(
                                document.toObject(
                                    TransactionModel::class.java
                                )
                            )
                            sharedPrefManager.putInvestmentReqList(list)

                            setupViewPager()
                            setupTabLayout()
                        }
                    } else Toast.makeText(
                        mContext,
                        constants.SOMETHING_WENT_WRONG_MESSAGE,
                        Toast.LENGTH_SHORT
                    ).show()


                }
                .addOnFailureListener {
                    utils.endLoadingAnimation()
                    Toast.makeText(mContext, it.message + "", Toast.LENGTH_SHORT).show()

                }

        }
    }


    override fun onBackPressed() {
        val viewPager = binding.viewPager
        if (viewPager.currentItem == 0) {
            super.onBackPressed()
        } else {
            viewPager.currentItem = viewPager.currentItem - 1
        }
    }

    fun withdrawdate() {
        val approvedWithdrawTransactions = sharedPrefManager.getWithdrawReqList()
            .filter { it.status == constants.TRANSACTION_STATUS_APPROVED }
        val lastApprovedWithdrawTransaction = approvedWithdrawTransactions
            .filter { it.transactionAt != null }
            .maxByOrNull { it.transactionAt!! }
        lastApprovedWithdrawTransaction?.let {
            val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
            val formattedDate = it.transactionAt!!.toDate()?.let { dateFormat.format(it) }
            binding.lastwithdrawDate.text = "$formattedDate to last withdraw"
        }
    }
    private fun showDatePickerDialog(check:String) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Create a DatePickerDialog
        val datePickerDialog = DatePickerDialog(
            mContext,
            { view, year, monthOfYear, dayOfMonth ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(Calendar.YEAR, year)
                selectedDate.set(Calendar.MONTH, monthOfYear)
                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
                val formattedDate = dateFormat.format(selectedDate.time)
                if(check=="From")
      binding.DateFrom.text=formattedDate
else if(check=="To")
    binding.DateTo.text=formattedDate
            },
            year,
            month,
            day
        )

        // Show the DatePickerDialog
        datePickerDialog.show()
    }
}
