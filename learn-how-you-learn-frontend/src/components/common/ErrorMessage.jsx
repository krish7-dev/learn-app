export default function ErrorMessage({ message }) {
  return <div className="error-box">{message || 'Something went wrong'}</div>
}
