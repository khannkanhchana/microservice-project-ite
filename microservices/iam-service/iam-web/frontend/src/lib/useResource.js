import { useCallback, useEffect, useRef, useState } from 'react'

/** Loads data with `fetcher` whenever `deps` change; exposes reload and local updates. */
export function useResource(fetcher, deps = []) {
  const [state, setState] = useState({ data: null, error: null, loading: true })
  const fetcherRef = useRef(fetcher)
  fetcherRef.current = fetcher

  const load = useCallback(async () => {
    setState((current) => ({ ...current, loading: true, error: null }))
    try {
      const data = await fetcherRef.current()
      setState({ data, error: null, loading: false })
    } catch (error) {
      setState((current) => ({ ...current, error, loading: false }))
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps)

  useEffect(() => {
    load()
  }, [load])

  const setData = useCallback((update) => {
    setState((current) => ({ ...current, data: typeof update === 'function' ? update(current.data) : update }))
  }, [])

  return { ...state, reload: load, setData }
}
