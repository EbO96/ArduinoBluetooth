package com.example.sebastian.brulinski.arduinobluetooth.Fragments

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import com.example.sebastian.brulinski.arduinobluetooth.R
import com.example.sebastian.brulinski.arduinobluetooth.databinding.FragmentTerminalBinding

class Terminal : Fragment() {

    private lateinit var binding: FragmentTerminalBinding
    private val sendText = StringBuilder()

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_terminal, container, false)

        //Edit text ime options
        binding.terminalEditText.setOnEditorActionListener { textView, actionId, keyEvent ->
            when (actionId) {
                EditorInfo.IME_ACTION_SEND -> {
                    sendToDevice("${binding.terminalEditText.text}")
                }
            }
            true
        }

        binding.sendMessageToDevice.setOnClickListener {
            sendToDevice("${binding.terminalEditText.text}")
        }

        binding.clearMessage.setOnClickListener{
            binding.terminalEditText.setText("")
        }

        return binding.root
    }

    private fun sendToDevice(text: String) {
        if(!text.isEmpty()){
            sendText.appendln(text)
            binding.terminalTextTextView.text = "$sendText\n"
        }
    }

}
