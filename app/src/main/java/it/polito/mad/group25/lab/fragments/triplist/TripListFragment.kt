package it.polito.mad.group25.lab.fragments.triplist

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import it.polito.mad.group25.lab.R
import it.polito.mad.group25.lab.SharedViewModel
import it.polito.mad.group25.lab.utils.entities.Trip
import it.polito.mad.group25.lab.utils.entities.startDateFormatted
import it.polito.mad.group25.lab.utils.entities.timeFormatted
import it.polito.mad.group25.lab.utils.fragment.showError


class TripListFragment : Fragment() {

    //Initializing sharedViewModel
    private val sharedViewModel: SharedViewModel by activityViewModels()


    private var columnCount = 1



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            columnCount = it.getInt(ARG_COLUMN_COUNT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.trip_list_fragment, container, false)


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val list = view.findViewById<RecyclerView>(R.id.list)
        //pass an observable to MyTripCarRecyclerViewAdapter
        sharedViewModel.tripList.observe( viewLifecycleOwner, { tripList ->
            // Set the adapter
            with(list) {
                layoutManager = when {
                    columnCount <= 1 -> LinearLayoutManager(context)
                    else -> GridLayoutManager(context, columnCount)
                }
                adapter = MyTripCardRecyclerViewAdapter(tripList)
            }
        })

        val addNewTripButton = view.findViewById<FloatingActionButton>(R.id.addTrip)
        addNewTripButton.setOnClickListener {
            var id = sharedViewModel.pushTrip(Trip())
            sharedViewModel.selectTrip(id)
            sharedViewModel.setNew(true)
            view.findNavController().navigate(R.id.showTripEditFragment)
        }
    }

    companion object {

        // TODO: Customize parameter argument names
        const val ARG_COLUMN_COUNT = "column-count"

        // TODO: Customize parameter initialization
        @JvmStatic
        fun newInstance(columnCount: Int) =
            TripListFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_COLUMN_COUNT, columnCount)
                }
            }
    }

    inner class MyTripCardRecyclerViewAdapter(val tripList: List<Trip>):
            RecyclerView.Adapter<MyTripCardRecyclerViewAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.trip_card_fragment, parent, false)
            return ViewHolder(view)
        }

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = tripList[position]

            holder.car_name.text = item.carName
            holder.departure_date.text = item.startDateFormatted()
            holder.departure_time.text = item.locations[0].timeFormatted()
            holder.departure_location.text = item.locations[0].location.toString()
            holder.arrival_time.text = item.locations[item.locations.size - 1].timeFormatted()
            holder.arrival_location.text = item.locations[item.locations.size - 1].location.toString()
            holder.seats.text = item.seats.toString()
            holder.price.text = item.price.toString()
            holder.editButton.setOnClickListener { view ->
                sharedViewModel.selectTrip(position)
                view.findNavController().navigate(R.id.showTripEditFragment)
            }
            holder.card.setOnClickListener{ view ->
                sharedViewModel.selectTrip(position)
                view.findNavController().navigate(R.id.showTripDetailsFragment)
            }
            if (tripList[position].carPic != null)
                holder.image.setImageDrawable(tripList[position].carPic)
        }

        override fun getItemCount(): Int = tripList.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val car_name: TextView = view.findViewById(R.id.card_car_name)
            val departure_date: TextView = view.findViewById(R.id.card_departure_date)
            val departure_time: TextView = view.findViewById(R.id.card_trip_time_departure)
            val departure_location: TextView = view.findViewById(R.id.card_trip_location_departure)
            val arrival_time: TextView = view.findViewById(R.id.card_trip_time_arrival)
            val arrival_location: TextView = view.findViewById(R.id.card_trip_location_arrival)
            val seats: TextView = view.findViewById(R.id.card_seats_text)
            val price: TextView = view.findViewById(R.id.card_price_text)
            val editButton: Button = view.findViewById(R.id.editTrip)
            val card: CardView = view.findViewById(R.id.card)
            val image: ImageView = view.findViewById(R.id.card_car_image)
        }
    }
}