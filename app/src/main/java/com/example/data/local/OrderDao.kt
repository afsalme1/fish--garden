package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.OrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Query("SELECT * FROM customer_orders ORDER BY timestamp DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM customer_orders WHERE customerPhone = :phone ORDER BY timestamp DESC")
    fun getOrdersByPhone(phone: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM customer_orders WHERE id = :id LIMIT 1")
    suspend fun getOrderById(id: Long): OrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Query("UPDATE customer_orders SET orderStatus = :status, adminNotes = :adminNotes WHERE id = :id")
    suspend fun updateOrderStatus(id: Long, status: String, adminNotes: String = "")

    @Delete
    suspend fun deleteOrder(order: OrderEntity)

    @Query("DELETE FROM customer_orders WHERE id = :id")
    suspend fun deleteOrderById(id: Long)

    @Query("SELECT COUNT(*) FROM customer_orders")
    suspend fun getOrderCount(): Int
}
