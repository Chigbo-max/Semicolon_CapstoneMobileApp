package com.antithefttracker.agent

import android.os.Bundle
import android.widget.*
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class LockActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.getBooleanExtra("unlock", false)) {
            finish() 
            return
        }

        // Force the screen to stay on
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Make sure this activity shows on top of everything
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)

        showBiometricPrompt()

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            gravity = Gravity.CENTER
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val title = TextView(this).apply {
            text = "Device Locked"
            setTextColor(Color.WHITE)
            textSize = 28f
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Enter PIN to Unlock"
            setTextColor(Color.GRAY)
            textSize = 18f
            gravity = Gravity.CENTER
        }

        val pinInput = EditText(this).apply {
            hint = "Enter 4-digit PIN"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            textSize = 20f
            gravity = Gravity.CENTER
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                        android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }

        val unlockBtn = Button(this).apply {
            text = "Unlock"
            setOnClickListener {
                val enteredPin = pinInput.text.toString()
                if (enteredPin == "1234") {
                    finish()
                } else {
                    Toast.makeText(this@LockActivity, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                    pinInput.text.clear()
                }
            }
        }

        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(pinInput)
        layout.addView(unlockBtn)

        setContentView(layout)
    }

    private fun showBiometricPrompt() {
        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )

        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            val executor = ContextCompat.getMainExecutor(this)
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Unlock")
                .setSubtitle("Use your fingerprint or device PIN")
                .setNegativeButtonText("Cancel")
                .build()

            val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    finish()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(this@LockActivity, "Biometric error: $errString", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(this@LockActivity, "Biometric failed", Toast.LENGTH_SHORT).show()
                }
            })

            biometricPrompt.authenticate(promptInfo)
        }
    }

    override fun onBackPressed() {
    }
}