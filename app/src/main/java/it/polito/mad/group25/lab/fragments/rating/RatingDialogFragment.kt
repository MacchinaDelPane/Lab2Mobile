package it.polito.mad.group25.lab.fragments.rating

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import com.google.android.material.textfield.TextInputEditText
import it.polito.mad.group25.lab.AuthenticationContext
import it.polito.mad.group25.lab.R
import it.polito.mad.group25.lab.fragments.review.Review
import it.polito.mad.group25.lab.fragments.review.ReviewViewModel
import it.polito.mad.group25.lab.fragments.trip.Trip

class RatingDialogFragment(
    val trip: Trip,
    val stars: Int,
    val isReviewedDriver: Boolean,
    val userToBeReviewed: String? = null
    ): DialogFragment() {

    private val authenticationContext: AuthenticationContext by activityViewModels()
    private val reviews: ReviewViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout to use as dialog or embedded fragment
        return inflater.inflate(R.layout.rate_trip_dialog_fragment, container, false)
    }

    /** The system calls this only when creating the layout in a dialog. */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // The only reason you might override this method when using onCreateView() is
        // to modify any dialog characteristics. For example, the dialog includes a
        // title by default, but your custom layout might not need it. So here you can
        // remove the dialog title, but you must call the superclass to get the Dialog.
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Button>(R.id.submit).setOnClickListener {
            //salva su firebase:
            //voto dell'utente(authentication.userId) al guidatore (trip.ownerId) --> stars
            //votante
            //votato
            //commento --> comment
            //trip
            if (isReviewedDriver){
                reviews.addReview(
                    authenticationContext.userId()!!,//votante
                    trip.ownerId!!,//votato
                    trip.id!!,
                    view.findViewById<TextInputEditText>(R.id.comment).text.toString(),
                    stars,
                    isReviewedDriver
                )
            } else {
                if (userToBeReviewed != null) {
                    reviews.addReview(
                        authenticationContext.userId()!!,//votante
                        userToBeReviewed,//votato
                        trip.id!!,
                        view.findViewById<TextInputEditText>(R.id.comment).text.toString(),
                        stars,
                        isReviewedDriver
                    )
                }
            }

            dialog?.dismiss()
        }
    }
}
