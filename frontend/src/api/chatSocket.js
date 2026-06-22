import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

let client = null
let pendingSubscription = null

export function connectChatSocket(token, { onConnect, onError } = {}) {
  if (!token) return null

  disconnectChatSocket()

  client = new Client({
    webSocketFactory: () => new SockJS('/ws'),
    connectHeaders: {
      Authorization: `Bearer ${token}`,
    },
    reconnectDelay: 3000,
    onConnect: () => {
      onConnect?.()
      if (pendingSubscription) {
        pendingSubscription()
        pendingSubscription = null
      }
    },
    onStompError: (frame) => onError?.(frame.headers?.message || 'STOMP error'),
  })

  client.activate()
  return client
}

export function subscribeConversation(conversationId, handler) {
  const topic = `/topic/conversation.${conversationId}`

  const doSubscribe = () => {
    if (!client?.connected) return () => {}
    const sub = client.subscribe(topic, (msg) => {
      try {
        handler(JSON.parse(msg.body))
      } catch {
        // ignore bad payload
      }
    })
    return () => sub.unsubscribe()
  }

  if (client?.connected) {
    return doSubscribe()
  }

  pendingSubscription = doSubscribe
  return () => {
    pendingSubscription = null
  }
}

export function disconnectChatSocket() {
  pendingSubscription = null
  if (client) {
    client.deactivate()
    client = null
  }
}