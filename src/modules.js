// Maelezo ya kila ukurasa. Majina ya "key" ni majina halisi ya columns za database yako.
const st = (...o) => o.map((v) => ({ value: v, label: v }))

export const MODULES = [
  {
    key: 'vendors', label: 'Wafanyabiashara', single: 'Mfanyabiashara', pk: 'vendor_id',
    columns: [
      { key: 'full_name', label: 'Jina' }, { key: 'phone', label: 'Simu' },
      { key: 'type_name', label: 'Aina' }, { key: 'space_name', label: 'Nafasi' },
      { key: 'status', label: 'Hali', badge: true },
    ],
    fields: [
      { key: 'first_name', label: 'Jina la kwanza', required: true },
      { key: 'middle_name', label: 'Jina la kati' },
      { key: 'last_name', label: 'Jina la mwisho', required: true },
      { key: 'national_id', label: 'Namba ya NIDA' },
      { key: 'phone', label: 'Simu', required: true },
      { key: 'address', label: 'Anwani' },
      { key: 'emergency_contact', label: 'Simu ya dharura' },
      { key: 'vendor_type_id', label: 'Aina ya biashara', type: 'select', lookup: 'vendorTypes', required: true },
      { key: 'space_type_id', label: 'Aina ya nafasi', type: 'select', lookup: 'spaceTypes', required: true },
      { key: 'status', label: 'Hali', type: 'select', options: st('Pending', 'Approved', 'Active', 'Inactive'), initial: 'Approved' },
    ],
  },
  {
    key: 'vendor-types', label: 'Aina za biashara', single: 'Aina ya biashara', pk: 'vendor_type_id',
    columns: [{ key: 'type_name', label: 'Jina' }, { key: 'description', label: 'Maelezo' }],
    fields: [
      { key: 'type_name', label: 'Jina la aina', required: true },
      { key: 'description', label: 'Maelezo', type: 'textarea' },
    ],
  },
  {
    key: 'stalls', label: 'Stalls', single: 'Stall', pk: 'stall_id',
    columns: [
      { key: 'stall_number', label: 'Namba' }, { key: 'zone_name', label: 'Zone' },
      { key: 'stall_type_name', label: 'Aina' }, { key: 'size', label: 'Ukubwa' },
      { key: 'status', label: 'Hali', badge: true },
    ],
    fields: [
      { key: 'stall_number', label: 'Namba ya stall', required: true, lockOnEdit: true },
      { key: 'zone_id', label: 'Zone', type: 'select', lookup: 'zones', required: true },
      { key: 'stall_type_id', label: 'Aina ya stall', type: 'select', lookup: 'stallTypes', required: true },
      { key: 'size', label: 'Ukubwa', type: 'number', required: true },
      { key: 'status', label: 'Hali', type: 'select', options: st('Vacant', 'Available', 'Occupied', 'Reserved', 'Maintenance'), initial: 'Vacant' },
    ],
  },
  {
    key: 'allocations', label: 'Ugawaji', single: 'Ugawaji', pk: 'allocation_id',
    columns: [
      { key: 'vendor_name', label: 'Mfanyabiashara' }, { key: 'stall_number', label: 'Stall' },
      { key: 'allocation_date', label: 'Tarehe' }, { key: 'end_date', label: 'Mwisho' },
      { key: 'status', label: 'Hali', badge: true },
    ],
    fields: [
      { key: 'vendor_id', label: 'Mfanyabiashara', type: 'select', lookup: 'vendors', required: true, filter: (o) => o.status === 'Approved' },
      { key: 'stall_id', label: 'Stall', type: 'select', lookup: 'stalls', required: true, filter: (o) => ['Vacant', 'Available'].includes(o.status) },
      { key: 'allocation_date', label: 'Tarehe ya ugawaji', type: 'date', required: true },
      { key: 'end_date', label: 'Tarehe ya mwisho', type: 'date' },
      { key: 'status', label: 'Hali', type: 'select', options: st('Active', 'Ended', 'Expired', 'Cancelled', 'Transferred'), initial: 'Active' },
      { key: 'remarks', label: 'Maelezo', type: 'textarea' },
    ],
  },
  {
    key: 'licenses', label: 'Leseni', single: 'Leseni', pk: 'license_id',
    columns: [
      { key: 'license_number', label: 'Namba' }, { key: 'vendor_name', label: 'Mfanyabiashara' },
      { key: 'issue_date', label: 'Ilitolewa' }, { key: 'expiry_date', label: 'Inaisha' },
      { key: 'status', label: 'Hali', badge: true },
    ],
    fields: [
      { key: 'vendor_id', label: 'Mfanyabiashara', type: 'select', lookup: 'vendors', required: true },
      { key: 'license_number', label: 'Namba ya leseni', required: true },
      { key: 'issue_date', label: 'Tarehe ya kutolewa', type: 'date', required: true },
      { key: 'expiry_date', label: 'Tarehe ya kuisha', type: 'date', required: true },
      { key: 'status', label: 'Hali', type: 'select', options: st('Active', 'Pending', 'Expired', 'Cancelled'), initial: 'Active' },
    ],
  },
  {
    key: 'payments', label: 'Malipo', single: 'Malipo', pk: 'payment_id', money: ['amount'],
    columns: [
      { key: 'receipt_number', label: 'Risiti' }, { key: 'vendor_name', label: 'Mfanyabiashara' },
      { key: 'payment_name', label: 'Aina' }, { key: 'amount', label: 'Kiasi', money: true },
      { key: 'payment_date', label: 'Tarehe' }, { key: 'status', label: 'Hali', badge: true },
    ],
    fields: [
      { key: 'vendor_id', label: 'Mfanyabiashara', type: 'select', lookup: 'vendors', required: true },
      { key: 'payment_type_id', label: 'Aina ya malipo', type: 'select', lookup: 'paymentTypes', required: true },
      { key: 'amount', label: 'Kiasi (TZS)', type: 'number', required: true },
      { key: 'receipt_number', label: 'Namba ya risiti', required: true },
      { key: 'payment_date', label: 'Tarehe ya malipo', type: 'date', required: true },
      { key: 'status', label: 'Hali', type: 'select', options: st('Paid', 'Pending', 'Cancelled'), initial: 'Paid' },
    ],
  },
  {
    key: 'announcements', label: 'Matangazo', single: 'Tangazo', pk: 'announcement_id',
    columns: [
      { key: 'title', label: 'Kichwa' }, { key: 'message', label: 'Ujumbe', clip: true },
      { key: 'author', label: 'Aliyeandika' }, { key: 'created_at', label: 'Tarehe', date: true },
    ],
    fields: [
      { key: 'title', label: 'Kichwa cha tangazo', required: true },
      { key: 'message', label: 'Ujumbe', type: 'textarea', required: true },
    ],
  },
]
