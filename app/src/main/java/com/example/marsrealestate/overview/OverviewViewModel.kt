package com.example.marsrealestate.overview

import android.util.Log
import androidx.databinding.InverseMethod
import androidx.lifecycle.*
import com.example.marsrealestate.R
import com.example.marsrealestate.data.MarsRepository
import com.example.marsrealestate.data.MarsProperty
import com.example.marsrealestate.data.network.MarsApiFilter
import com.example.marsrealestate.data.network.MarsApiPropertySorting
import com.example.marsrealestate.data.network.MarsApiQuery
import com.example.marsrealestate.util.Event
import com.example.marsrealestate.util.Result
import kotlinx.coroutines.*
import java.lang.Exception
import java.lang.NumberFormatException
import java.util.Collections.addAll
import kotlin.random.Random


class OverviewViewModel(private val repository : MarsRepository) : ViewModel() {

    private val _status = MutableLiveData<Result<Nothing>>()
    val status: LiveData<Result<Nothing>> = _status

    private val _endOfData = MutableLiveData<Boolean>(false)
    val endOfData = _endOfData

    private val _navigateToProperty = MutableLiveData<Event<MarsProperty>>()
    val navigateToProperty: LiveData<Event<MarsProperty>> = _navigateToProperty

    //Ugly but necessary because otherwise the loadNextPage function is triggered too often, notably three times
    // on startup because there are 3 default properties
    private var ignoreFirstFilterChange = true
    private var ignoreFirstQueryStringChange = true
    private var ignoreFirstSortedByFilterChange = true

    val filter = MutableLiveData<MarsApiFilter.MarsPropertyType>(MarsApiFilter.MarsPropertyType.ALL)
    val queryString = MutableLiveData<String>("")
    val sortedBy = MutableLiveData<MarsApiPropertySorting>(MarsApiPropertySorting.Default)


    private val _properties = MediatorLiveData<MutableList<MarsProperty>>().apply {
        value = mutableListOf()

        addSource(filter) { if (ignoreFirstFilterChange) ignoreFirstFilterChange = false else loadNextPage(true) }
        addSource(queryString) { if (ignoreFirstQueryStringChange) ignoreFirstQueryStringChange = false else loadNextPage(true) }
        addSource(sortedBy) { if (ignoreFirstSortedByFilterChange) ignoreFirstSortedByFilterChange = false else loadNextPage(true) }
    }

    val properties : LiveData<List<MarsProperty>> = _properties.map { it }



//    private val _properties = MutableLiveData<MutableList<MarsProperty>>()
//    val properties : LiveData<List<MarsProperty>> = _properties.map { it }


    private val itemsPerPage = 7
    private var pageLoadedCount = 0


    init {
        loadNextPage(true)
    }


    fun updateQueryString(str : String?) {
        if (queryString.value != str) {
            queryString.value = str ?: ""
        }
    }

    fun clearQueryStringAndUpdate() = updateQueryString(null)


    fun loadNextPage(reset : Boolean = false) {
        _status.postValue(Result.Loading())
        Log.d("############","########${Random.nextInt(10)}")
        viewModelScope.launch {

            try {
                val pageToLoad = if (reset) 1 else pageLoadedCount + 1

                val newProps = requestNewPropertiesFromRepo(pageToLoad,itemsPerPage,sortedBy.value ?: MarsApiPropertySorting.Default)

                if (reset) {
                    //Replacing the previous list in case of reset
                    _properties.postValue(newProps.toMutableList())
                }
                else if (newProps.isNotEmpty()) {
                    //Adding new elements if no reset
                    val currentProps = _properties.value ?: mutableListOf()
                    currentProps.addAll(newProps)
                    _properties.postValue(currentProps)
                }


                if (_endOfData.value != newProps.isEmpty())
                    _endOfData.postValue(newProps.isEmpty())

                //If we got new properties, we successfully loaded the page, so the page number increases
                if (newProps.isNotEmpty())
                    pageLoadedCount = pageToLoad

                _status.postValue(Result.Success())
            }

            catch (e : Exception) {
                _status.postValue(Result.Error())
//                _properties.postValue(mutableListOf())
                Log.e(this@OverviewViewModel::class.simpleName,Log.getStackTraceString(e))
            }
        }
    }

    private suspend fun requestNewPropertiesFromRepo(pageToLoad: Int, itemsPerPage: Int, sorting: MarsApiPropertySorting = MarsApiPropertySorting.Default) : List<MarsProperty> {
        val query = MarsApiQuery(
            pageToLoad, itemsPerPage,
            MarsApiFilter(
                filter.value ?: MarsApiFilter.MarsPropertyType.ALL,
                queryString.value ?: ""))

        return repository.getProperties(query,sorting )
    }

    fun displayPropertyDetails(marsProperty: MarsProperty) {
        _navigateToProperty.value = Event(marsProperty)
    }

}





class OverviewViewModelFactory(private val repository: MarsRepository) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return OverviewViewModel(repository) as T
    }
}


//
//object SortingConverter {
//    @InverseMethod("expirationYearToString")
//    @JvmStatic
//    fun stringToSorting(value: String): Int {
//        return try {
//            val a = if (value == MarsApiPropertySorting.PriceAscending) R.string.priceAscending else R.string.priceDescending
//            return context.getString(a)
//        } catch (e: NumberFormatException) {
//            -1
//        }
//    }
//
//    @JvmStatic
//    fun sortingToString(value: MarsApiPropertySorting): String =
//        if (value > 0) value.toString() else ""
//
//}