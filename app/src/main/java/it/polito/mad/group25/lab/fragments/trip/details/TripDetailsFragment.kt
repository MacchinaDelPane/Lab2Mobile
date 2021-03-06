package it.polito.mad.group25.lab.fragments.trip.details

import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.View.*
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import it.polito.mad.group25.lab.AuthenticationContext
import it.polito.mad.group25.lab.R
import it.polito.mad.group25.lab.fragments.map.MapViewModel
import it.polito.mad.group25.lab.fragments.rating.RatingDialogFragment
import it.polito.mad.group25.lab.fragments.review.Review
import it.polito.mad.group25.lab.fragments.review.ReviewViewModel
import it.polito.mad.group25.lab.fragments.trip.*
import it.polito.mad.group25.lab.fragments.userprofile.UserProfileViewModel
import it.polito.mad.group25.lab.utils.fragment.showError
import it.polito.mad.group25.lab.utils.toLocalDateTime
import it.polito.mad.group25.lab.utils.views.fromBlob
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

abstract class TripDetailsFragment(
    contentLayoutId: Int
) : Fragment(contentLayoutId) {

    private val tripViewModel: TripViewModel by activityViewModels()
    private val authenticationContext: AuthenticationContext by activityViewModels()
    private val userProfileViewModel: UserProfileViewModel by activityViewModels()
    private val mapViewModel: MapViewModel by activityViewModels()
    private val reviewViewModel: ReviewViewModel by activityViewModels()

    private var isOwner = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        if (authenticationContext.userId() == tripViewModel.trip.value?.ownerId)
            isOwner = true
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (isOwner && tripViewModel.trip.value?.isEditable() == true)
            inflater.inflate(R.menu.menu, menu)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)
        val context = this.context

        val rv = view.findViewById<RecyclerView>(R.id.tripList)
        val additionalInfoChips = view.findViewById<ChipGroup>(R.id.additionalInfoChips)

        tripViewModel.trip.observe(viewLifecycleOwner, { trip ->
            reviewViewModel.reviews.observe(viewLifecycleOwner, { reviewMap ->
                if (trip != null) {

                    //populate view with trip **********************************************************
                    view.findViewById<TextView>(R.id.carName).text = trip.carName
                    view.findViewById<TextView>(R.id.departureDate).text = trip.startDateFormatted()
                    view.findViewById<TextView>(R.id.seatsText).text = trip.seats.toString()
                    view.findViewById<TextView>(R.id.priceText).text = trip.price.toString()

                    val last = trip.locations.lastIndex
                    view.findViewById<TextView>(R.id.durationText).text =
                        getDurationFormatted(
                            trip.locations[0].locationTime.toLocalDateTime(),
                            trip.locations[last].locationTime.toLocalDateTime()
                        )

                    rv.layoutManager = LinearLayoutManager(context)
                    rv.adapter = TripLocationAdapter(trip.locations)

                    if (additionalInfoChips.childCount != 0)
                        additionalInfoChips.removeAllViews()

                    if (trip.additionalInfo.size == 0) {
                        additionalInfoChips.visibility = GONE
                        view.findViewById<TextView>(R.id.noOtherInfos).visibility = VISIBLE
                    } else {
                        trip.additionalInfo.forEach {
                            val chip = Chip(context)
                            chip.text = it
                            additionalInfoChips.addView(chip)
                        }
                    }
                    trip.carPic?.let {
                        view.findViewById<ImageView>(R.id.carImage)
                            .fromBlob(it)
                    }

                    val div = view.findViewById<View>(R.id.dividerInfo)
                    val intUserText = view.findViewById<TextView>(R.id.interestedUsers)
                    val rv2 = view.findViewById<RecyclerView>(R.id.userList)

                    if (!isOwner) {
                        //normal user
                        val fab = view.findViewById<FloatingActionButton>(R.id.tripDetailsFab)

                        fab.visibility = VISIBLE
                        rv2.visibility = GONE
                        div.visibility = GONE
                        intUserText.visibility = GONE

                        fab.setOnClickListener {
                            val user =
                                trip.interestedUsers.find { it.userId == authenticationContext.userId()!! }
                            if (user != null)
                                if (user.isConfirmed)
                                    showError("Request confirmed, you already are in!")
                                else
                                    showError("Request already sent, still waiting for confirmation")
                            else {
                                if (trip.seats > 0) {
                                    showError("Sent confirmation request to the trip's owner")
                                    tripViewModel.addCurrentUserToSet(authenticationContext.userId()!!)
                                } else showError("No more seats available!")
                            }
                        }

                    } else {
                        //trip owner
                        if (trip.interestedUsers.size == 0) {
                            //no interested users
                            view.findViewById<TextView>(R.id.noIntUsers).visibility = VISIBLE
                            rv2.visibility = GONE

                        } else {
                            //at least one interested user
                            rv2.layoutManager = LinearLayoutManager(context)
                            rv2.adapter = TripUsersAdapter(
                                trip.interestedUsers.toList(),
                                userProfileViewModel,
                                trip,
                                reviewMap.values.toList()
                            )

                        }
                    }
                    //**********************************************************************************

                    // mapButton
                    view.findViewById<ImageButton>(R.id.mapButton).setOnClickListener {
                        navigateToMapFragment()
                    }
                }
            })
        })
    }

    fun navigateToUserProfile(userId: String) {
        userProfileViewModel.showUser(userId)
        requireActivity().findNavController(R.id.nav_host_fragment_content_main)
            .navigate(R.id.action_showTripDetailsFragment_to_showUserProfileFragment)
    }

    private fun navigateToMapFragment(){
        mapViewModel.geopoints = tripViewModel.trip.value!!.locations
            .filter { tripLoc -> tripLoc.latitude != null && tripLoc.longitude != null }
            .toMutableList()
        requireActivity().findNavController(R.id.nav_host_fragment_content_main)
            .navigate(R.id.action_showTripDetailsFragment_to_ShowDetailsMap)
    }

    inner class TripUsersAdapter(
        private val list: List<TripUser>,
        private val usersData: UserProfileViewModel,
        val trip: Trip,
        val reviews: List<Review>
    ) :
        RecyclerView.Adapter<TripUsersAdapter.TripUsersViewHolder>() {

        inner class TripUsersViewHolder(v: View, private val usersData: UserProfileViewModel) :
            RecyclerView.ViewHolder(v) {
            private val username: TextView = v.findViewById(R.id.username)
            private val proPic: ImageView = v.findViewById(R.id.proPic)
            private val confirmCheck: CheckBox = v.findViewById(R.id.confirm_user)
            private val confirmedIcon: ImageView = v.findViewById(R.id.confirmedIcon)
            private val rating = v.findViewById<RatingBar>(R.id.ratingBar2)

            fun bind(user: TripUser) {
                val tripFinished = Date(trip.locations[trip.locations.size-1].locationTime).before(Date())
                usersData.showUser(user.userId)
                usersData.shownUser.observe(viewLifecycleOwner, { user_ ->
                    user_.fullName?.let { username.text = it }
                    user_.userProfilePhotoFile?.let { proPic.fromBlob(it) }
                })

                username.setOnClickListener {
                    navigateToUserProfile(user.userId)
                }
                proPic.setOnClickListener {
                    navigateToUserProfile(user.userId)
                }

                confirmCheck.visibility = INVISIBLE

                if (user.isConfirmed && !tripFinished)
                    confirmedIcon.visibility = VISIBLE
                val review = reviews.filter { review ->
                    //Log.d("test", "${user.userId}")
                    review.reviewed == user.userId && review.tripId == trip.id
                }
                if(tripFinished) {
                    confirmCheck.visibility = INVISIBLE
                    confirmedIcon.visibility = INVISIBLE
                    rating.visibility = VISIBLE
                    //Log.d("test", "$review")
                    if(review.isNotEmpty()){
                        rating.rating = review[0].stars?.toFloat()!!
                        rating.setIsIndicator(true)
                    } else {

                        rating.setOnRatingBarChangeListener { ratingBar, rating, fromUser ->  RatingDialogFragment(trip, rating.toInt(),false, user.userId).show(childFragmentManager, "RatingDialogFragment") }
                    }
                }
                else {
                    rating.visibility = GONE
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripUsersViewHolder {
            val layout = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
            return TripUsersViewHolder(layout, usersData)
        }

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onBindViewHolder(holder: TripUsersViewHolder, position: Int) {
            holder.bind(list[position])
        }

        override fun getItemCount(): Int = list.size

        override fun getItemViewType(position: Int): Int = R.layout.trip_user_line

    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun getDurationFormatted(first: LocalDateTime, last: LocalDateTime): String {
    val durationMin = ChronoUnit.MINUTES.between(first, last).toInt()
    val minDay = 24 * 60

    val days = durationMin / minDay
    val hours = durationMin % minDay / 60
    val min = durationMin % minDay % 60

    return "${if (days != 0) "${days}d" else ""} ${if (hours != 0) "${hours}h" else ""} ${if (min != 0) "${min}min" else ""}"
}

class TripLocationAdapter(private val list: List<TripLocation>) :
    RecyclerView.Adapter<TripLocationAdapter.TripLocationViewHolder>() {

    class TripLocationViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private val location: TextView = v.findViewById(R.id.trip_location)
        private val time: TextView = v.findViewById(R.id.trip_time)

        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(t: TripLocation) {
            location.text = t.location
            time.text = t.timeFormatted()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripLocationViewHolder {
        val layout = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return TripLocationViewHolder(layout)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: TripLocationViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int = list.size

    override fun getItemViewType(position: Int): Int {
        //caso limite iniziale: solo una tappa
        if (list.size == 1) return R.layout.trip_departure_line
        //altrimenti
        return when (position) {
            0 -> R.layout.trip_departure_line
            list.size - 1 -> R.layout.trip_destination_line
            else -> R.layout.trip_line
        }
    }
}


