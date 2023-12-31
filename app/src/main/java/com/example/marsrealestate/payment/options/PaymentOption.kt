package com.example.marsrealestate.payment.options

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class PaymentOption : Parcelable{
    abstract fun getLabelHidden() : String

}


@Parcelize
data class VisaCard (
    val firstName: String,
    val lastName: String,
    val cardNumber: String,
    val expirationMonth: Int,
    val expirationYear: Int,
    val secretCode: String
) : PaymentOption(), Parcelable {

    companion object {
        const val VISA_CARD_NUMBER_LENGTH = 16
        const val VISA_SECRET_CODE_LENGTH = 3
    }

    override fun getLabelHidden(): String {
        if (cardNumber.length != VISA_CARD_NUMBER_LENGTH)
            return "Wrong format"

        val lastDigits = cardNumber.drop(VISA_CARD_NUMBER_LENGTH - 4)
        return "... xxxx $lastDigits"
    }
}

