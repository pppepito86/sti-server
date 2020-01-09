import React from 'react';
import Workers from './Workers';

const AdminDashboardContent = () => {
  return (
    <div className="content-wrapper" style={{ minHeight: '550px' }}>
      <section className="content">
        <div className="row">
          <div className="col-md-6">
            <Workers.Workers />
          </div>
        </div>
      </section>
    </div>
  )
}


export default AdminDashboardContent
