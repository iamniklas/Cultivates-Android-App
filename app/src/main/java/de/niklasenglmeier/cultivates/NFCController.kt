package de.niklasenglmeier.cultivates

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.widget.Toast
import java.io.UnsupportedEncodingException
import kotlin.experimental.and

class NFCController(activity: Activity, _callbacks: INFCCallbacks? = null) {

    private val callbacks = _callbacks
    private var mNFCAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity.applicationContext)
    private lateinit var mPendingIntent: PendingIntent
    private lateinit var mWritingTagFilters: Array<IntentFilter>

    init {
        if(mNFCAdapter == null) {
            Toast.makeText(activity.applicationContext, "No NFC Support", Toast.LENGTH_SHORT).show()
        }
        else {
            readFromIntent(activity.intent)

            mPendingIntent = PendingIntent.getActivity(
                activity.applicationContext, 0, Intent(activity.applicationContext, activity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                ), 0
            )
            val tagDetected = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
            tagDetected.addCategory(Intent.CATEGORY_DEFAULT)
            mWritingTagFilters = arrayOf(tagDetected)
        }
    }

    fun readFromIntent(intent: Intent) {
        val action = intent.action
        if (NfcAdapter.ACTION_TAG_DISCOVERED == action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == action ||
            NfcAdapter.ACTION_NDEF_DISCOVERED == action) {
            val rawMsg = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            val msgs = mutableListOf<NdefMessage>()
            if (rawMsg != null) {
                for (msg in rawMsg) {
                    msgs.add(msg as NdefMessage)
                }
            }
            buildTagViews(msgs)
        }
    }

    private fun buildTagViews(msgs: MutableList<NdefMessage>) {
        if (msgs.size == 0) return

        var text = ""
        val payload = msgs[0].records[0].payload
        //val textEncoding = if ((payload[0] and 128.toByte()) == (0.toByte())) "UTF-8" else "UTF-16"
        val languageCodeLength = payload[0] and 63

        try {
            text = String(payload, languageCodeLength + 1, payload.size - languageCodeLength - 1)
        }
        catch (e: UnsupportedEncodingException) {

        }

        callbacks?.onNFCRead(text)
    }
}