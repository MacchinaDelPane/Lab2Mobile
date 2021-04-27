package it.polito.mad.group25.lab.fragments.trip.details

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import it.polito.mad.group25.lab.R
import it.polito.mad.group25.lab.fragments.trip.TripLocation
import it.polito.mad.group25.lab.fragments.trip.TripViewModel
import it.polito.mad.group25.lab.fragments.trip.startDateFormatted
import it.polito.mad.group25.lab.fragments.trip.timeFormatted
import it.polito.mad.group25.lab.utils.views.fromFile
import java.io.File
import java.time.LocalTime
import java.time.temporal.ChronoUnit

abstract class TripDetailsFragment(
    contentLayoutId: Int
) : Fragment(contentLayoutId) {

    private val tripViewModel: TripViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)
        val context = this.context

        val rv = view.findViewById<RecyclerView>(R.id.tripList)
        val additionalInfoChips = view.findViewById<ChipGroup>(R.id.additionalInfoChips)

        val trip = tripViewModel.trip

        view.findViewById<TextView>(R.id.carName).text = trip.carName
        view.findViewById<TextView>(R.id.departureDate).text = trip.startDateFormatted()
        view.findViewById<TextView>(R.id.seatsText).text = trip.seats.toString()
        view.findViewById<TextView>(R.id.priceText).text = trip.price.toString()

        val last = trip.locations.lastIndex
        view.findViewById<TextView>(R.id.durationText).text =
            getDurationFormatted(trip.locations[0].locationTime, trip.locations[last].locationTime)

        rv.layoutManager = LinearLayoutManager(context)
        rv.adapter = TripLocationAdapter(trip.locations)

        if (additionalInfoChips.childCount != 0)
            additionalInfoChips.removeAllViews()

        trip.additionalInfo.forEach {
            val chip = Chip(context)
            chip.text = it
            additionalInfoChips.addView(chip)
        }
        view.findViewById<ImageView>(R.id.carImage)
            .fromFile(trip.carPic ?: File(requireActivity().dataDir, trip.id.toString()))

    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun getDurationFormatted(first: LocalTime, last: LocalTime): String {
    val durationMin = ChronoUnit.MINUTES.between(first, last).toInt()
    val hours = durationMin / 60
    val min = durationMin % 60

    return "${if (hours != 0) "$hours h" else ""} ${if (min != 0) "$min min" else ""}"
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