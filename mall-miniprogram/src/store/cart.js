import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { cartApi } from '@/api/cart'
import { useUserStore } from './user'

/**
 * 购物车状态
 *
 * 字段与后端 CartVO 对齐：
 * id / productId / quantity / selected(0-未选中 1-选中) / productName
 * / productImage / productPrice / productStock / productStatus / totalPrice
 */
export const useCartStore = defineStore('cart', () => {
  const items = ref([])
  const loading = ref(false)

  const userStore = useUserStore()

  const selectedItems = computed(() =>
    items.value.filter((item) => Number(item.selected) === 1)
  )

  const totalCount = computed(() =>
    items.value.reduce((sum, item) => sum + Number(item.quantity || 0), 0)
  )

  const selectedCount = computed(() =>
    selectedItems.value.reduce((sum, item) => sum + Number(item.quantity || 0), 0)
  )

  const selectedAmount = computed(() =>
    selectedItems.value.reduce(
      (sum, item) => sum + Number(item.productPrice || 0) * Number(item.quantity || 0),
      0
    )
  )

  const allSelected = computed(
    () => items.value.length > 0 && selectedItems.value.length === items.value.length
  )

  /** 加载购物车列表；未登录时直接置空 */
  async function load() {
    if (!userStore.isLogin) {
      items.value = []
      return []
    }
    loading.value = true
    try {
      const res = await cartApi.getList()
      items.value = res.data || []
      return items.value
    } finally {
      loading.value = false
    }
  }

  async function add(productId, quantity = 1) {
    if (!userStore.ensureLogin()) {
      return
    }
    await cartApi.add(productId, quantity)
    await load()
  }

  async function updateQuantity(productId, quantity) {
    await cartApi.updateQuantity(productId, quantity)
    await load()
  }

  async function toggleSelected(productId, selected) {
    await cartApi.updateSelected(productId, selected ? 1 : 0)
    await load()
  }

  async function toggleAll(selected) {
    await cartApi.updateAllSelected(selected ? 1 : 0)
    await load()
  }

  async function remove(productId) {
    await cartApi.remove(productId)
    await load()
  }

  async function clear() {
    await cartApi.clear()
    items.value = []
  }

  /** 退出登录时重置本地状态 */
  function reset() {
    items.value = []
  }

  return {
    items,
    loading,
    selectedItems,
    totalCount,
    selectedCount,
    selectedAmount,
    allSelected,
    load,
    add,
    updateQuantity,
    toggleSelected,
    toggleAll,
    remove,
    clear,
    reset
  }
})
